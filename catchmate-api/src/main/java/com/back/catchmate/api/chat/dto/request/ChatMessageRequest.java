package com.back.catchmate.api.chat.dto.request;

import com.back.catchmate.application.chat.dto.command.ChatMessageCommand;
import com.back.catchmate.domain.chat.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {

    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;

    @NotNull(message = "메시지 타입은 필수입니다.")
    private MessageType messageType;

    public ChatMessageCommand toCommand(Long senderId) {
        return new ChatMessageCommand(chatRoomId, senderId, content, messageType);
    }
}
