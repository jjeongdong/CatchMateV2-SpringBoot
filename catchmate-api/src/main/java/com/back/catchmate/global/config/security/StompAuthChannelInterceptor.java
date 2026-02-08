package com.back.catchmate.global.config.security;

import com.back.catchmate.orchestration.auth.AuthOrchestrator;
import error.ErrorCode;
import error.exception.BaseException;
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

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final AuthOrchestrator authOrchestrator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // CONNECT 프레임: 토큰 검증 및 인증 설정
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token == null) {
                token = accessor.getFirstNativeHeader("authorization");
            }
            if (token == null) {
                token = accessor.getFirstNativeHeader("token");
            }

            if (!StringUtils.hasText(token)) {
                log.warn("WebSocket CONNECT missing token/header");
                throw new BaseException(ErrorCode.SOCKET_CONNECT_FAILED);
            }

            try {
                Long userId = authOrchestrator.getUserId(token);
                String role = authOrchestrator.getUserRole(token);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                accessor.setUser(authentication);
                log.debug("WebSocket user authenticated: {}", userId);
            } catch (Exception e) {
                log.warn("WebSocket token validation failed: {}", e.getMessage());
                throw new BaseException(ErrorCode.SOCKET_CONNECT_FAILED);
            }
        }

        // SUBSCRIBE 프레임: 채팅방 참가 여부 확인
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            // Destination 예: /sub/chat/room/{chatRoomId}
            String dest = accessor.getDestination();
            if (dest != null && dest.startsWith("/sub/chat/room/")) {
                String idPart = dest.substring("/sub/chat/room/".length());
                try {
                    Long chatRoomId = Long.parseLong(idPart);

                    Authentication user = (Authentication) accessor.getUser();
                    if (user == null || user.getPrincipal() == null) {
                        log.warn("Unauthenticated SUBSCRIBE attempt to {}", dest);
                        throw new BaseException(ErrorCode.SOCKET_CONNECT_FAILED);
                    }

                    Long userId;
                    try {
                        userId = Long.parseLong(user.getPrincipal().toString());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid principal format: {}", user.getPrincipal());
                        throw new BaseException(ErrorCode.SOCKET_CONNECT_FAILED);
                    }

//                    boolean participant = chatUseCase.canAccessChatRoom(userId, chatRoomId);
//                    if (!participant) {
//                        log.warn("User {} tried to subscribe to chatRoom {} without participation", userId, chatRoomId);
//                        throw new BaseException(ErrorCode.USER_CHATROOM_NOT_FOUND);
//                    }

                } catch (NumberFormatException e) {
                    log.warn("Invalid chatRoomId in destination: {}", dest);
                    throw new BaseException(ErrorCode.BAD_REQUEST);
                }
            }
        }

        return message;
    }
}
