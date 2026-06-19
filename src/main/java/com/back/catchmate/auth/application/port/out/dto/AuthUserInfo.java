package com.back.catchmate.auth.application.port.out.dto;

public record AuthUserInfo(
        Long userId,
        String authority
) {
}
