package com.back.catchmate.application.auth.dto.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthLoginCommand {
    private String providerIdWithProvider;
    private String fcmToken;
}
