package com.back.catchmate.enroll.application.event;

public record EnrollRejectedEvent(
        Long enrollId,
        Long boardId,
        Long applicantId,
        Long boardOwnerId
) {
    public static EnrollRejectedEvent of(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        return new EnrollRejectedEvent(enrollId, boardId, applicantId, boardOwnerId);
    }
}
