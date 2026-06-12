package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.user.domain.model.User;
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
        LocalDateTime updatedAt
) {
    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getNickName(),
                user.getProvider().name(),
                user.getGender(),
                user.getBirthDate(),
                user.getClub().getName(),
                user.getWatchStyle(),
                user.getAuthority().name(),
                user.isReported(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
