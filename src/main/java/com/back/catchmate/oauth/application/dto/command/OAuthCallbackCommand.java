package com.back.catchmate.oauth.application.dto.command;

import com.back.catchmate.oauth.domain.enums.Provider;

public record OAuthCallbackCommand(
        Provider provider,
        String code,
        String state,
        String stateFromCookie
) {
}
