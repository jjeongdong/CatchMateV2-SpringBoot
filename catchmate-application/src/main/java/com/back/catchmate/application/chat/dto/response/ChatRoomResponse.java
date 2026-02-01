package com.back.catchmate.application.chat.dto.response;

import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.domain.chat.model.ChatRoom;
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
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getId())
                .board(BoardResponse.from(chatRoom.getBoard(), false))
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    public static ChatRoomResponse from(ChatRoom chatRoom, ChatMessageResponse lastMessage) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getId())
                .board(BoardResponse.from(chatRoom.getBoard(), false))
                .lastMessage(lastMessage)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
