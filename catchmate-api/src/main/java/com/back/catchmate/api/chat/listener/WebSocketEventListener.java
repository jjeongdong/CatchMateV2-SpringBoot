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

/**
 * WebSocket 연결/해제 이벤트를 감지하여 사용자 온라인 상태를 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final UserOnlineStatusOrchestrator userOnlineStatusOrchestrator;
    private final ChatOrchestrator chatOrchestrator;

    /**
     * WebSocket 연결 성공 시 사용자를 온라인 상태로 설정
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication user = (Authentication) headerAccessor.getUser();

        if (user != null && user.getPrincipal() != null) {
            try {
                Long userId = Long.parseLong(user.getPrincipal().toString());
                userOnlineStatusOrchestrator.setUserOnline(userId);
                log.info("WebSocket connected - User {} set to ONLINE", userId);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse userId from principal: {}", user.getPrincipal());
            }
        }
    }

    /**
     * WebSocket 구독 이벤트 (채팅방 입장 감지)
     * 사용자가 /sub/chat/room/{roomId} 토픽을 구독하면 해당 방의 메시지를 모두 읽음 처리함
     */
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication user = (Authentication) headerAccessor.getUser();
        String destination = headerAccessor.getDestination();

        // destination 예시: /sub/chat/room/10
        if (user != null && destination != null && destination.contains("/chat/room/")) {
            try {
                Long userId = Long.parseLong(user.getPrincipal().toString());
                Long roomId = extractRoomIdFromDestination(destination);

                if (roomId != null) {
                    chatOrchestrator.readChatRoom(userId, roomId);
                    log.info("User {} subscribed to room {}. Marked as read.", userId, roomId);
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse userId or roomId. Dest: {}, User: {}", destination, user.getPrincipal());
            } catch (Exception e) {
                log.error("Error while handling subscribe event", e);
            }
        }
    }

    /**
     * WebSocket 연결 해제 시 사용자를 오프라인 상태로 설정
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication user = (Authentication) headerAccessor.getUser();

        if (user != null && user.getPrincipal() != null) {
            try {
                Long userId = Long.parseLong(user.getPrincipal().toString());
                userOnlineStatusOrchestrator.setUserOffline(userId);
                log.info("WebSocket disconnected - User {} set to OFFLINE", userId);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse userId from principal: {}", user.getPrincipal());
            }
        }
    }

    private Long extractRoomIdFromDestination(String destination) {
        try {
            // 경로의 마지막 부분을 ID로 간주
            int lastIndex = destination.lastIndexOf("/");
            if (lastIndex != -1) {
                String idStr = destination.substring(lastIndex + 1);
                return Long.parseLong(idStr);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }
}
