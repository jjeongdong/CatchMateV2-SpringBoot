package com.back.catchmate.enroll.application.dto.result;

public record EnrollAcceptResult(Long enrollId, String message) {
    public static EnrollAcceptResult of(Long enrollId) {
        return new EnrollAcceptResult(enrollId, "직관 신청을 수락했습니다.");
    }
}
