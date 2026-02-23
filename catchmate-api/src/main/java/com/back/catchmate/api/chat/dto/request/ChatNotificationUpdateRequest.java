package com.back.catchmate.api.chat.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatNotificationUpdateRequest {
    private boolean isOn;
}
