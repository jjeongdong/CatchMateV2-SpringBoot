package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.board.application.dto.response.BoardResponse;
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
    public static ChatRoomResponse from(ChatRoom chatRoom, ChatMessageResponse lastMessage, Long unreadCount, boolean isNotificationOn, boolean readOnly) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getBoard() != null ? BoardResponse.from(chatRoom.getBoard(), false) : null,
                lastMessage,
                unreadCount,
                chatRoom.getChatRoomImageUrl(),
                isNotificationOn,
                readOnly,
                chatRoom.getCreatedAt()
        );
    }
}
