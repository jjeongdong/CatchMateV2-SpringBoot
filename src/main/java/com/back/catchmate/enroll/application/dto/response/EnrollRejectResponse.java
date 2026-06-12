package com.back.catchmate.enroll.application.dto.response;


public record EnrollRejectResponse(
        Long enrollId,
        String message
) {
    public static EnrollRejectResponse of(Long enrollId) {
        return new EnrollRejectResponse(enrollId, "직관 신청을 거절했습니다.");
    }
}
