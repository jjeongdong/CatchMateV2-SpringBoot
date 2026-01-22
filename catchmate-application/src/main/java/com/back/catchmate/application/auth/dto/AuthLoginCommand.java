package com.back.catchmate.application.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthLoginCommand {
    private String providerIdWithProvider;
    private String fcmToken;
}
