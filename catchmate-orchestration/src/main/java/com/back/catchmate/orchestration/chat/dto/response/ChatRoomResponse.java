package com.back.catchmate.orchestration.chat.dto.response;

import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.orchestration.board.dto.response.BoardResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ChatRoomResponse {
    private Long chatRoomId;
    private BoardResponse board;
    private ChatMessageResponse lastMessage;
    private Long unreadCount;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getId())
                .board(BoardResponse.from(chatRoom.getBoard(), false))
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, ChatMessageResponse lastMessage, Long unreadCount) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getId())
                .board(BoardResponse.from(chatRoom.getBoard(), false))
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
