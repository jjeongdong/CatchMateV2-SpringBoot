package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.user.domain.model.User;

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
    public static AdminUserResponse from(User user, Club club) {
        return new AdminUserResponse(
                user.getId(),
                user.getProfileImageUrl(),
                user.getNickName(),
                user.getEmail(),
                club != null ? club.getName() : null,
                user.getGender().toString(),
                user.getAuthority().name(),
                user.getCreatedAt()
        );
    }
}
