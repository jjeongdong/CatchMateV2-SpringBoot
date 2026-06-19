package com.back.catchmate.auth.application.dto;

public record SignupTokenPayload(
        String provider,
        String providerId,
        String email,
        String profileImageUrl
) {
}
