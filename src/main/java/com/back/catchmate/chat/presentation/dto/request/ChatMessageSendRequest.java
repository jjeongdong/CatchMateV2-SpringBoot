package com.back.catchmate.chat.presentation.dto.request;

import com.back.catchmate.chat.application.dto.command.ChatMessageSendCommand;
import com.back.catchmate.chat.domain.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageSendRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.") Long chatRoomId,
        @NotBlank(message = "메시지 내용은 필수입니다.") String content,
        @NotNull(message = "메시지 타입은 필수입니다.") MessageType messageType) {
    public ChatMessageSendCommand toCommand() {
        return new ChatMessageSendCommand(chatRoomId, content, messageType);
    }
}
