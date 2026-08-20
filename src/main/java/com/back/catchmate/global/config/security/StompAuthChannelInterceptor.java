package com.back.catchmate.global.config.security;

import com.back.catchmate.auth.application.port.in.AuthInternalQueryUseCase;
import com.back.catchmate.chat.application.port.in.ChatClientQueryUseCase;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private static final List<String> TOKEN_HEADERS = List.of("Authorization", "authorization", "token");

    private static final String CHAT_ROOM_DESTINATION_PREFIX = "/sub/chat/room/";

    // WebSocketConfig 의 setApplicationDestinationPrefixes("/pub") 와 짝을 이룬다.
    private static final String APPLICATION_DESTINATION_PREFIX = "/pub/";

    // 서버가 개인 큐로 보내는 목적지. 실시간 알림(NotificationRedisSubscriber)과
    // 채팅 전송 실패 통보(ChatController 의 @MessageExceptionHandler) 두 가지다.
    private static final Set<String> ALLOWED_USER_DESTINATIONS = Set.of(
            "/user/queue/notifications",
            "/user/queue/errors"
    );

    private final AuthInternalQueryUseCase authInternalQueryUseCase;
    private final ChatClientQueryUseCase chatClientQueryUseCase;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            authorizeSend(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket CONNECT missing token/header");
            throw new BaseException(ErrorCode.SOCKET_CONNECT_FAILED);
        }

        try {
            Long userId = authInternalQueryUseCase.getUserId(token);
            String role = authInternalQueryUseCase.getUserRole(token);

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority(role))
            ));
            log.debug("WebSocket user authenticated: {}", userId);
        } catch (Exception e) {
            log.warn("WebSocket token validation failed: {}", e.getMessage());
            throw new BaseException(ErrorCode.SOCKET_CONNECT_FAILED);
        }
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        for (String header : TOKEN_HEADERS) {
            String token = accessor.getFirstNativeHeader(header);
            if (token != null) {
                return token;
            }
        }
        return null;
    }

    // 브로커(DefaultSubscriptionRegistry)는 구독 목적지를 AntPathMatcher 패턴으로 취급한다.
    // 따라서 "이 prefix 로 시작할 때만 검사" 방식은 /sub/** 같은 와일드카드에 그대로 뚫린다
    // (권한 검사를 건너뛴 채 등록되고, 이후 /sub/chat/room/{id} 전송 전부에 매칭된다).
    // 서버가 실제로 전송하는 목적지는 채팅방과 개인 큐뿐이므로 화이트리스트로 막는다.
    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String dest = accessor.getDestination();
        if (dest == null) {
            log.warn("SUBSCRIBE without destination");
            throw new BaseException(ErrorCode.BAD_REQUEST);
        }

        if (dest.startsWith(CHAT_ROOM_DESTINATION_PREFIX)) {
            authorizeChatRoomSubscribe(accessor, dest);
            return;
        }

        if (!ALLOWED_USER_DESTINATIONS.contains(dest)) {
            log.warn("Rejected SUBSCRIBE to disallowed destination: {}", dest);
            throw new BaseException(ErrorCode.BAD_REQUEST);
        }
    }

    private void authorizeChatRoomSubscribe(StompHeaderAccessor accessor, String dest) {
        Long chatRoomId = parseChatRoomId(dest);
        Long userId = requireAuthenticatedUserId(accessor, dest);

        if (!chatClientQueryUseCase.canAccessChatRoom(userId, chatRoomId)) {
            log.warn("User {} tried to subscribe to chatRoom {} without participation", userId, chatRoomId);
            throw new BaseException(ErrorCode.USER_CHATROOM_NOT_FOUND);
        }
    }

    // 브로커 prefix(/sub·/queue)로 온 SEND 는 @MessageMapping 을 거치지 않고 SimpleBroker 가 구독자에게
    // 그대로 릴레이한다. 즉 컨트롤러·서비스의 멤버십 검사가 실행될 기회 자체가 없으므로, 클라이언트가
    // 쓸 수 있는 목적지를 애플리케이션 prefix 로 좁혀 모든 전송이 서비스를 거치도록 강제한다.
    private void authorizeSend(StompHeaderAccessor accessor) {
        String dest = accessor.getDestination();
        if (dest == null || !dest.startsWith(APPLICATION_DESTINATION_PREFIX)) {
            log.warn("Rejected SEND to non-application destination: {}", dest);
            throw new BaseException(ErrorCode.BAD_REQUEST);
        }
    }

    private Long parseChatRoomId(String dest) {
        try {
            return Long.parseLong(dest.substring(CHAT_ROOM_DESTINATION_PREFIX.length()));
        } catch (NumberFormatException e) {
            log.warn("Invalid chatRoomId in destination: {}", dest);
            throw new BaseException(ErrorCode.BAD_REQUEST);
        }
    }

    private Long requireAuthenticatedUserId(StompHeaderAccessor accessor, String dest) {
        Authentication user = (Authentication) accessor.getUser();
        if (user == null || user.getPrincipal() == null) {
            log.warn("Unauthenticated SUBSCRIBE attempt to {}", dest);
            throw new BaseException(ErrorCode.SOCKET_CONNECT_FAILED);
        }

        try {
            return Long.parseLong(user.getPrincipal().toString());
        } catch (NumberFormatException e) {
            log.warn("Invalid principal format: {}", user.getPrincipal());
            throw new BaseException(ErrorCode.SOCKET_CONNECT_FAILED);
        }
    }
}
