package com.back.catchmate.enroll.application.event;

public record EnrollCancelledEvent(
        Long enrollId,
        Long boardId,
        Long applicantId,
        Long boardOwnerId
) {
    public static EnrollCancelledEvent of(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        return new EnrollCancelledEvent(enrollId, boardId, applicantId, boardOwnerId);
    }
}
