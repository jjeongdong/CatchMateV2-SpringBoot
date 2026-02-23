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
    private boolean isNotificationOn;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom chatRoom, ChatMessageResponse lastMessage, Long unreadCount, boolean isNotificationOn) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getId())
                .board(BoardResponse.from(chatRoom.getBoard(), false))
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .isNotificationOn(isNotificationOn)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
