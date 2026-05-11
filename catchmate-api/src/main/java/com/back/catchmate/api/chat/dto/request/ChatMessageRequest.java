package com.back.catchmate.api.chat.dto.request;

import com.back.catchmate.orchestration.chat.dto.command.ChatMessageCommand;
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

    public ChatMessageCommand toCommand(Long senderId) {
        return new ChatMessageCommand(chatRoomId, senderId, content);
    }
}
