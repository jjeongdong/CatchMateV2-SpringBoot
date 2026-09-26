package com.back.catchmate.user.application.dto.result;

import com.back.catchmate.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUserDetailResult(
        Long userId,
        String email,
        String nickName,
        String provider,
        Character gender,
        LocalDate birthDate,
        String clubName,
        String watchStyle,
        String role,
        boolean reported,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static AdminUserDetailResult of(User user, String clubName) {
        return new AdminUserDetailResult(
                user.getId(),
                user.getEmail(),
                user.getNickName(),
                user.getProvider(),
                user.getGender(),
                user.getBirthDate(),
                clubName,
                user.getWatchStyle(),
                user.getAuthority().name(),
                user.isReported(),
                user.getCreatedAt(),
                user.getModifiedAt());
    }
}
