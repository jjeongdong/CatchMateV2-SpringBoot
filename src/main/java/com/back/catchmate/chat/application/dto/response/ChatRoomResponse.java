package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.chat.domain.model.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        BoardResponse board,
        ChatMessageResponse lastMessage,
        Long unreadCount,
        String chatRoomImageUrl,
        boolean isNotificationOn,
        boolean readOnly,
        LocalDateTime createdAt
) {
    public static ChatRoomResponse from(ChatRoom chatRoom, BoardResponse boardResponse, ChatMessageResponse lastMessage, Long unreadCount, boolean isNotificationOn, boolean readOnly) {
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
