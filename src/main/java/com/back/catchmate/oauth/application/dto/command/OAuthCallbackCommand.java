package com.back.catchmate.oauth.application.dto.command;

import com.back.catchmate.user.domain.enums.Provider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OAuthCallbackCommand {
    private final Provider provider;
    private final String code;
    private final String state;
    private final String stateFromCookie;
}
