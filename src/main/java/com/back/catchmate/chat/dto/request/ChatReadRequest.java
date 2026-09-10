package com.back.catchmate.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatReadRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.") Long chatRoomId
) {
}
