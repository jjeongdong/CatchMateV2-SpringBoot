package com.back.catchmate.chat.presentation;

import com.back.catchmate.chat.application.ChatQueryService;
import com.back.catchmate.global.config.security.StompSubscriptionAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 채팅방 구독(/sub/chat/room/{id})은 그 방의 활성 멤버만 허용한다.
@Component
@RequiredArgsConstructor
public class ChatRoomSubscriptionAuthorizer implements StompSubscriptionAuthorizer {
    private static final String CHAT_ROOM_DESTINATION_PREFIX = "/sub/chat/room/";

    private final ChatQueryService chatQueryService;

    @Override
    public boolean supports(String destination) {
        return destination.startsWith(CHAT_ROOM_DESTINATION_PREFIX);
    }

    @Override
    public void authorize(Long userId, String destination) {
        chatQueryService.verifySubscription(userId, destination.substring(CHAT_ROOM_DESTINATION_PREFIX.length()));
    }
}
