package com.back.catchmate.api.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatReadRequest {
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;
    @NotNull(message = "메시지 ID는 필수입니다.")
    private Long messageId;
}
