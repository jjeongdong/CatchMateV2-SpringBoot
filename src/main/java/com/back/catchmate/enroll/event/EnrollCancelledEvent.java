package com.back.catchmate.enroll.event;

public record EnrollCancelledEvent(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
    public static EnrollCancelledEvent of(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        return new EnrollCancelledEvent(enrollId, boardId, applicantId, boardOwnerId);
    }
}
