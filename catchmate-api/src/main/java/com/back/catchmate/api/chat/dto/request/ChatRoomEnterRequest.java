package com.back.catchmate.api.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomEnterRequest {

    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;
}
