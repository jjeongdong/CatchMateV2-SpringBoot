package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        ChatRoomBoardSummary board,
        ChatMessageResponse lastMessage,
        Long unreadCount,
        String chatRoomImageUrl,
        @JsonProperty("notificationOn")
        boolean isNotificationOn,
        boolean readOnly,
        LocalDateTime createdAt
) {
    public static ChatRoomResponse from(ChatRoom chatRoom, ChatRoomBoardSummary boardResponse, ChatMessageResponse lastMessage, Long unreadCount, boolean isNotificationOn, boolean readOnly) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                boardResponse,
                lastMessage,
                unreadCount,
                chatRoom.getChatRoomImageUrl(),
                isNotificationOn,
                readOnly,
                chatRoom.getCreatedAt()
        );
    }
}
