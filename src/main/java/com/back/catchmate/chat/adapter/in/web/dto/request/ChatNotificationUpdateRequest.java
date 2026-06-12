package com.back.catchmate.chat.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatNotificationUpdateRequest(
        @JsonProperty("isOn") boolean isOn
) {
}
