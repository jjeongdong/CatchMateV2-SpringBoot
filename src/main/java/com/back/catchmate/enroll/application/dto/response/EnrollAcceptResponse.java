package com.back.catchmate.enroll.application.dto.response;


public record EnrollAcceptResponse(
        Long enrollId,
        String message
) {
    public static EnrollAcceptResponse of(Long enrollId) {
        return new EnrollAcceptResponse(enrollId, "직관 신청을 수락했습니다.");
    }
}
