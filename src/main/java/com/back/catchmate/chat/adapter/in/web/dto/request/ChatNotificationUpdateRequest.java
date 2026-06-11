package com.back.catchmate.chat.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatNotificationUpdateRequest {
    @JsonProperty("isOn")
    private boolean isOn;
}
