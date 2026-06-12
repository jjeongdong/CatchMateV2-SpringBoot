package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.enroll.domain.model.Enroll;
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
    public static AdminEnrollmentResponse from(Enroll enroll) {
        return new AdminEnrollmentResponse(
                enroll.getId(),
                enroll.getUser().getId(),
                enroll.getUser().getProfileImageUrl(),
                enroll.getUser().getNickName(),
                enroll.getUser().getClub().getName(),
                enroll.getUser().getGender(),
                enroll.getUser().getEmail(),
                enroll.getUser().getProvider().name(),
                enroll.getAcceptStatus().name(),
                enroll.getRequestedAt()
        );
    }
}
