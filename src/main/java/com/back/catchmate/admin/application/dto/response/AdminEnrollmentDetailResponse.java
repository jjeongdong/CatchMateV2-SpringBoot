package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminEnrollInfo;
import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;

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
        LocalDateTime requestedAt
) {
    public static AdminEnrollmentDetailResponse from(AdminEnrollInfo enroll, AdminUserInfo user, String clubName) {
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
                enroll.requestedAt()
        );
    }
}
