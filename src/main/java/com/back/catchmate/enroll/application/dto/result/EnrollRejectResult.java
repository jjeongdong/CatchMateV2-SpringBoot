package com.back.catchmate.enroll.application.dto.result;

public record EnrollRejectResult(Long enrollId, String message) {
    public static EnrollRejectResult of(Long enrollId) {
        return new EnrollRejectResult(enrollId, "직관 신청을 거절했습니다.");
    }
}
