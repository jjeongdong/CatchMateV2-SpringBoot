package com.back.catchmate.admin.dto.response;

import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUserDetailResponse(
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
    public static AdminUserDetailResponse from(UserInfo user, String clubName) {
        return new AdminUserDetailResponse(
                user.userId(),
                user.email(),
                user.nickName(),
                user.provider(),
                user.gender(),
                user.birthDate(),
                clubName,
                user.watchStyle(),
                user.authority(),
                user.reported(),
                user.createdAt(),
                user.updatedAt());
    }
}
