package com.back.catchmate.orchestration.oauth.dto.response;

public sealed interface OAuthCallbackResult permits OAuthCallbackResult.Existing, OAuthCallbackResult.NewUser {

    record Existing(String accessToken, String refreshToken) implements OAuthCallbackResult {}

    record NewUser(String signupToken) implements OAuthCallbackResult {}
}
