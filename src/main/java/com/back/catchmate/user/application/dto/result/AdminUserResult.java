package com.back.catchmate.user.application.dto.result;

import com.back.catchmate.user.domain.User;
import java.time.LocalDateTime;

public record AdminUserResult(
        Long userId,
        String profileImageUrl,
        String nickName,
        String email,
        String clubName,
        String gender,
        String authority,
        LocalDateTime createdAt) {
    public static AdminUserResult of(User user, String clubName) {
        return new AdminUserResult(
                user.getId(),
                user.getProfileImageUrl(),
                user.getNickName(),
                user.getEmail(),
                clubName,
                user.getGender() != null ? user.getGender().toString() : null,
                user.getAuthority().name(),
                user.getCreatedAt());
    }
}
