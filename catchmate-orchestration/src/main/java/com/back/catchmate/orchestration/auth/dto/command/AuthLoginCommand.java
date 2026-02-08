package com.back.catchmate.orchestration.auth.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthLoginCommand {
    private String providerIdWithProvider;
    private String fcmToken;
}
