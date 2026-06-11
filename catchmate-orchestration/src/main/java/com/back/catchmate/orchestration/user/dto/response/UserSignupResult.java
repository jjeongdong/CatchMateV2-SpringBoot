package com.back.catchmate.orchestration.user.dto.response;

public record UserSignupResult(UserRegisterResponse response, String refreshToken) {}
