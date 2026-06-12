package com.back.catchmate.chat.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatRoomEnterRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.") Long chatRoomId
) {
}
