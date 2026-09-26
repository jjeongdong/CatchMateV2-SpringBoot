package com.back.catchmate.chat.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

// JSON 키는 옛 요청과 같은 notificationOn. 빠지면 false 로 바뀌지 않게 Boolean + @NotNull 로 받는다.
public record ChatNotificationUpdateRequest(
        @JsonProperty("notificationOn") @NotNull(message = "알림 설정 값은 필수입니다.") Boolean notificationOn) {}
