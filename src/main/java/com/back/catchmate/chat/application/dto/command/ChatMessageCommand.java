package com.back.catchmate.chat.application.dto.command;

import com.back.catchmate.chat.domain.enums.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageCommand {
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private MessageType messageType;

    public ChatMessageCommand(Long chatRoomId, Long senderId, String content, MessageType messageType) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
    }
}
