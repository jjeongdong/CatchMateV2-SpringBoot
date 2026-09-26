package com.back.catchmate.admin.dto.response;

import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId,
        String profileImageUrl,
        String nickName,
        String email,
        String clubName,
        String gender,
        String authority,
        LocalDateTime createdAt) {
    public static AdminUserResponse from(UserInfo user, String clubName) {
        return new AdminUserResponse(
                user.userId(),
                user.profileImageUrl(),
                user.nickName(),
                user.email(),
                clubName,
                user.gender() != null ? user.gender().toString() : null,
                user.authority(),
                user.createdAt());
    }
}
