package com.back.catchmate.auth.application.dto.result;

public sealed interface OAuthLoginResult permits OAuthLoginResult.Existing, OAuthLoginResult.NewUser {

    record Existing(String accessToken, String refreshToken) implements OAuthLoginResult {}

    record NewUser(String signupToken) implements OAuthLoginResult {}
}
