package com.back.catchmate.enroll.application.event;

public record EnrollAcceptedEvent(
        Long enrollId,
        Long boardId,
        Long applicantId,
        Long boardOwnerId
) {
    public static EnrollAcceptedEvent of(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        return new EnrollAcceptedEvent(enrollId, boardId, applicantId, boardOwnerId);
    }
}
