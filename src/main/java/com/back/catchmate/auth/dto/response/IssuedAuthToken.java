package com.back.catchmate.auth.dto.response;

/**
 * 발급된 access/refresh 토큰 페어.
 */
public record IssuedAuthToken(String accessToken, String refreshToken) {}
