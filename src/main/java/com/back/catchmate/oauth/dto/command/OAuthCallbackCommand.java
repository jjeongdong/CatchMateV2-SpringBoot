package com.back.catchmate.oauth.dto.command;

import com.back.catchmate.oauth.entity.Provider;

public record OAuthCallbackCommand(
        Provider provider,
        String code,
        String state,
        String stateFromCookie
) {
}
