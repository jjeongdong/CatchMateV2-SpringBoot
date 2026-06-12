package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.user.domain.model.User;

import java.time.LocalDateTime;

public record AdminEnrollmentResponse(
        Long enrollId,
        Long userId,
        String profileImageUrl,
        String nickName,
        String clubName,
        Character gender,
        String email,
        String provider,
        String status,
        LocalDateTime requestedAt
) {
    public static AdminEnrollmentResponse from(Enroll enroll, User user, Club club) {
        return new AdminEnrollmentResponse(
                enroll.getId(),
                user.getId(),
                user.getProfileImageUrl(),
                user.getNickName(),
                club != null ? club.getName() : null,
                user.getGender(),
                user.getEmail(),
                user.getProvider().name(),
                enroll.getAcceptStatus().name(),
                enroll.getRequestedAt()
        );
    }
}
