package com.back.catchmate.admin.application.port.out.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUserInfo(
        Long userId,
        String email,
        String provider,
        Character gender,
        String nickName,
        LocalDate birthDate,
        String watchStyle,
        String profileImageUrl,
        String authority,
        Long clubId,
        boolean reported,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
