package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.domain.model.Board;
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
    public static ChatRoomResponse from(ChatRoom chatRoom, Board board, ChatMessageResponse lastMessage, Long unreadCount, boolean isNotificationOn, boolean readOnly) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                board != null ? BoardResponse.from(board, false) : null,
                lastMessage,
                unreadCount,
                chatRoom.getChatRoomImageUrl(),
                isNotificationOn,
                readOnly,
                chatRoom.getCreatedAt()
        );
    }
}
