package com.back.catchmate.enroll.application.event;

public record EnrollRequestedEvent(
        Long enrollId,
        Long boardId,
        Long applicantId,
        Long boardOwnerId
) {
    public static EnrollRequestedEvent of(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        return new EnrollRequestedEvent(enrollId, boardId, applicantId, boardOwnerId);
    }
}
