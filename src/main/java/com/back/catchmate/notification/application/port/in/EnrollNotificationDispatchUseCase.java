package com.back.catchmate.notification.application.port.in;

public interface EnrollNotificationDispatchUseCase {
    void dispatchOnEnrollRequested(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void dispatchOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void dispatchOnEnrollRejected(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void dispatchOnEnrollCancelled(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);
}
