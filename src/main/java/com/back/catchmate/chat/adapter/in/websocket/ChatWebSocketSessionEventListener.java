package com.back.catchmate.chat.adapter.in.websocket;

import com.back.catchmate.chat.application.port.in.ChatClientCommandUseCase;
import com.back.catchmate.chat.application.port.out.external.UserOnlineStatusCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketSessionEventListener {
    private final ChatClientCommandUseCase chatClientCommandUseCase;
    private final UserOnlineStatusCommandPort userOnlineStatusCommandPort;

    /**
     * WebSocket 연결 (브라우저 접속)
     * 앱을 처음 켰을 때, 혹은 브라우저를 새로고침했을 때 발생하는 이벤트
     * 이전 세션의 비정상 종료로 인해 남아있는 stale focus room 을 초기화하고, 온라인 상태를 설정한다.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);

        if (userId != null) {
            userOnlineStatusCommandPort.setUserOnline(userId);
            userOnlineStatusCommandPort.removeUserFocusRoom(userId);
            log.info("WebSocket connected - User {} set to ONLINE (focus reset)", userId);
        }
    }

    /**
     * WebSocket 연결 해제 (브라우저 종료 등)
     * 앱을 종료하거나 브라우저를 닫았을 때 발생하는 이벤트
     * 온라인 상태를 해제하고, 포커스 룸 정보를 제거한다.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);

        if (userId != null) {
            userOnlineStatusCommandPort.setUserOffline(userId);
            userOnlineStatusCommandPort.removeUserFocusRoom(userId);
            log.info("WebSocket disconnected - User {} set to OFFLINE", userId);
        }
    }

    /**
     * WebSocket 구독 (채팅방 입장)
     */
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);
        String destination = headerAccessor.getDestination();

        if (userId != null && destination != null && destination.contains("/chat/room/")) {
            Long roomId = extractRoomIdFromDestination(destination);
            if (roomId != null) {
                chatClientCommandUseCase.readChatRoom(userId, roomId);
                userOnlineStatusCommandPort.setUserFocusRoom(userId, roomId);
                log.info("User {} is focusing room {}", userId, roomId);
            }
        }
    }

    /**
     * WebSocket 구독 취소 (채팅방 퇴장 / 로비 이동)
     */
    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);

        if (userId != null) {
            userOnlineStatusCommandPort.removeUserFocusRoom(userId);
            log.info("User {} left the room (Focus removed)", userId);
        }
    }

    /**
     * StompHeaderAccessor에서 userId 추출
     */
    private Long extractUserId(StompHeaderAccessor accessor) {
        Authentication user = (Authentication) accessor.getUser();
        if (user != null) {
            try {
                return Long.parseLong(user.getName());
            } catch (NumberFormatException e) {
                log.warn("Invalid userId format: {}", user.getName());
            }
        }
        return null;
    }

    /**
     * destination 문자열에서 roomId 추출
     */
    private Long extractRoomIdFromDestination(String destination) {
        try {
            int lastIndex = destination.lastIndexOf("/");
            if (lastIndex != -1) {
                return Long.parseLong(destination.substring(lastIndex + 1));
            }
        } catch (Exception e) {
            log.warn("Failed to extract roomId from {}", destination);
        }
        return null;
    }
}
