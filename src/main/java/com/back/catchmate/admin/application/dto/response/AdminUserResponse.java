package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId,
        String profileImageUrl,
        String nickName,
        String email,
        String clubName,
        String gender,
        String authority,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(AdminUserInfo user, String clubName) {
        return new AdminUserResponse(
                user.userId(),
                user.profileImageUrl(),
                user.nickName(),
                user.email(),
                clubName,
                user.gender() != null ? user.gender().toString() : null,
                user.authority(),
                user.createdAt()
        );
    }
}
