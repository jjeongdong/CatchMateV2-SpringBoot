package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.board.application.dto.response.BoardResponse;
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
    private String chatRoomImageUrl;
    private boolean isNotificationOn;
    private boolean readOnly;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom chatRoom, ChatMessageResponse lastMessage, Long unreadCount, boolean isNotificationOn, boolean readOnly) {
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getId())
                .board(chatRoom.getBoard() != null ? BoardResponse.from(chatRoom.getBoard(), false) : null)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .chatRoomImageUrl(chatRoom.getChatRoomImageUrl())
                .isNotificationOn(isNotificationOn)
                .readOnly(readOnly)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
