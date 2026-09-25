package com.back.catchmate.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatNotificationUpdateRequest(@JsonProperty("notificationOn") boolean isNotificationOn) {}
