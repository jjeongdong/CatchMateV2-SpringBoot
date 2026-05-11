package com.back.catchmate.orchestration.chat.dto.command;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageCommand {
    private Long chatRoomId;
    private Long senderId;
    private String content;

    public ChatMessageCommand(Long chatRoomId, Long senderId, String content) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.content = content;
    }
}
