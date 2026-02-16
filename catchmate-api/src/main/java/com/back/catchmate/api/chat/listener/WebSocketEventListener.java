package com.back.catchmate.api.chat.listener;

import com.back.catchmate.orchestration.chat.ChatOrchestrator;
import com.back.catchmate.orchestration.user.UserOnlineStatusOrchestrator;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final ChatOrchestrator chatOrchestrator;
    private final UserOnlineStatusOrchestrator userOnlineStatusOrchestrator;

    /**
     * WebSocket 연결 성공
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);

        if (userId != null) {
            userOnlineStatusOrchestrator.setUserOnline(userId);
            log.info("WebSocket connected - User {} set to ONLINE", userId);
        }
    }

    /**
     * WebSocket 연결 해제 (브라우저 종료 등)
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);

        if (userId != null) {
            // 온라인 상태 해제
            userOnlineStatusOrchestrator.setUserOffline(userId);
            // 포커스 룸 정보 제거
            userOnlineStatusOrchestrator.removeUserFocusRoom(userId);
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
                chatOrchestrator.readChatRoom(userId, roomId);
                userOnlineStatusOrchestrator.setUserFocusRoom(userId, roomId);
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
            userOnlineStatusOrchestrator.removeUserFocusRoom(userId);
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
