package com.back.catchmate.api.chat.listener;

import com.back.catchmate.orchestration.user.UserOnlineStatusOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 연결/해제 이벤트를 감지하여 사용자 온라인 상태를 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final UserOnlineStatusOrchestrator userOnlineStatusOrchestrator;

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
}
