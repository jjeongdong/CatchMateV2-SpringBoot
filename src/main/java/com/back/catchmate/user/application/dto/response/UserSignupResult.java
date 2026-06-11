package com.back.catchmate.user.application.dto.response;

public record UserSignupResult(UserRegisterResponse response, String refreshToken) {}
