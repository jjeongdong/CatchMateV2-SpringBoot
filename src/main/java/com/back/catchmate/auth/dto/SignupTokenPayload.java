package com.back.catchmate.auth.dto;

public record SignupTokenPayload(String provider, String providerId, String email, String profileImageUrl) {}
