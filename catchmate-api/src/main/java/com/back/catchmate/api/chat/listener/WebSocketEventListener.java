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
    private final UserOnlineStatusOrchestrator userOnlineStatusOrchestrator;
    private final ChatOrchestrator chatOrchestrator;
    private final Map<String, Map<String, Long>> sessionRoomMap = new ConcurrentHashMap<>();

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
     * WebSocket 구독 취소 (채팅방 퇴장 / 로비 이동)
     */
    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();

        if (userId != null && sessionId != null && subscriptionId != null) {
            // 저장해둔 맵에서 RoomId 찾기
            Map<String, Long> subs = sessionRoomMap.get(sessionId);
            if (subs != null && subs.containsKey(subscriptionId)) {
                Long roomId = subs.remove(subscriptionId);

                // userOnlineStatusOrchestrator.leaveChatRoom(userId, roomId); // 구현 필요 시 주석 해제

                log.info("User {} left room {} (Unsubscribed).", userId, roomId);
            }
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
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();

        if (userId != null && destination != null && destination.contains("/chat/room/")) {
            Long roomId = extractRoomIdFromDestination(destination);
            if (roomId != null) {
                // 1. 읽음 처리
                chatOrchestrator.readChatRoom(userId, roomId);

                // 2. [추가] "현재 이 방에 있음" 상태 저장 (알림 방지용)
                // userOnlineStatusOrchestrator.enterChatRoom(userId, roomId); // 구현 필요 시 주석 해제

                // 3. [추가] Unsubscribe 처리를 위해 매핑 정보 저장
                sessionRoomMap.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                        .put(subscriptionId, roomId);

                log.info("User {} entered room {}. (SubId: {})", userId, roomId, subscriptionId);
            }
        }
    }

    /**
     * WebSocket 연결 해제 (브라우저 종료 등)
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(headerAccessor);
        String sessionId = headerAccessor.getSessionId();

        if (userId != null) {
            userOnlineStatusOrchestrator.setUserOffline(userId);
            // 메모리 누수 방지를 위해 맵 정리
            sessionRoomMap.remove(sessionId);
            log.info("WebSocket disconnected - User {} set to OFFLINE", userId);
        }
    }

    /**
     * StompHeaderAccessor에서 userId 추출
     */
    private Long extractUserId(StompHeaderAccessor accessor) {
        Authentication user = (Authentication) accessor.getUser();
        if (user != null) {
            try {
                // user.getPrincipal().toString() 대신 user.getName() 사용 권장
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
