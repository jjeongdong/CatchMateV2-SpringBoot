package com.back.catchmate.chat.presentation;

import com.back.catchmate.chat.application.ChatCommandService;
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

// WebSocket 세션 사건을 포커스 방(푸시 억제)과 읽음 처리로 옮긴다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionEventHandler {
    private static final String CHAT_ROOM_DESTINATION = "/chat/room/";

    private final ChatCommandService chatCommandService;

    // 이전 세션이 비정상 종료돼 남은 포커스를 지운다 (남아 있으면 그 방 푸시가 계속 억제된다).
    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        Long userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()));
        if (userId != null) {
            chatCommandService.unfocusChatRoom(userId);
            log.info("WebSocket connected - User {} (focus reset)", userId);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Long userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()));
        if (userId != null) {
            chatCommandService.unfocusChatRoom(userId);
            log.info("WebSocket disconnected - User {} (focus reset)", userId);
        }
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        String destination = accessor.getDestination();
        if (userId == null || destination == null || !destination.contains(CHAT_ROOM_DESTINATION)) {
            return;
        }
        Long chatRoomId = extractChatRoomId(destination);
        if (chatRoomId != null) {
            chatCommandService.readChatRoom(userId, chatRoomId);
            chatCommandService.focusChatRoom(userId, chatRoomId);
            log.info("User {} is focusing room {}", userId, chatRoomId);
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        Long userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()));
        if (userId != null) {
            chatCommandService.unfocusChatRoom(userId);
            log.info("User {} left the room (Focus removed)", userId);
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication user)) {
            return null;
        }
        try {
            return Long.parseLong(user.getName());
        } catch (NumberFormatException e) {
            log.warn("Invalid userId format: {}", user.getName());
            return null;
        }
    }

    // 구독 권한 검사(ChatRoomSubscriptionAuthorizer)를 통과한 주소라 여기서는 실패를 로그로만 남긴다.
    private Long extractChatRoomId(String destination) {
        try {
            return Long.parseLong(destination.substring(destination.lastIndexOf('/') + 1));
        } catch (NumberFormatException e) {
            log.warn("Failed to extract roomId from {}", destination);
            return null;
        }
    }
}
