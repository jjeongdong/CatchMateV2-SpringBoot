package com.back.catchmate.admin.dto.response;

import com.back.catchmate.enroll.dto.response.EnrollSummary;
import com.back.catchmate.user.dto.response.UserSummary;
import java.time.LocalDateTime;

public record AdminEnrollmentDetailResponse(
        Long enrollId,
        Long userId,
        String profileImageUrl,
        String nickName,
        String clubName,
        Character gender,
        String email,
        String provider,
        String status,
        LocalDateTime requestedAt) {
    public static AdminEnrollmentDetailResponse from(EnrollSummary enroll, UserSummary user, String clubName) {
        return new AdminEnrollmentDetailResponse(
                enroll.enrollId(),
                user.userId(),
                user.profileImageUrl(),
                user.nickName(),
                clubName,
                user.gender(),
                user.email(),
                user.provider(),
                enroll.acceptStatus(),
                enroll.requestedAt());
    }
}
