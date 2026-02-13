package com.back.catchmate.application.chat.event;

public record ChatReadEvent(
        Long chatRoomId,
        Long userId
) {
    public static ChatReadEvent of(Long chatRoomId, Long userId) {
        return new ChatReadEvent(chatRoomId, userId);
    }
}
