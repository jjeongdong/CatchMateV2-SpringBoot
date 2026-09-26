package com.back.catchmate.global.config.security;

public record AuthenticatedUser(Long userId, String role) {}
