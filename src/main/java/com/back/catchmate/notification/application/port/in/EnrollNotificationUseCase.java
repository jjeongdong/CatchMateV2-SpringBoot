package com.back.catchmate.notification.application.port.in;

public interface EnrollNotificationUseCase {
    void saveOnEnrollRequested(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void dispatchOnEnrollRequested(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void saveOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void dispatchOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void saveOnEnrollRejected(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void dispatchOnEnrollRejected(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void saveOnEnrollCancelled(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);

    void dispatchOnEnrollCancelled(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId);
}
