package com.back.catchmate.notification.event;

import com.back.catchmate.enroll.event.EnrollAcceptedEvent;
import com.back.catchmate.enroll.event.EnrollCancelledEvent;
import com.back.catchmate.enroll.event.EnrollRejectedEvent;
import com.back.catchmate.enroll.event.EnrollRequestedEvent;
import com.back.catchmate.notification.service.EnrollNotificationDispatchService;
import com.back.catchmate.notification.service.EnrollNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {
    private final EnrollNotificationService enrollNotificationService;
    private final EnrollNotificationDispatchService enrollNotificationDispatchService;

    @EventListener
    public void onSaveRequested(EnrollRequestedEvent event) {
        enrollNotificationService.saveOnEnrollRequested(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @EventListener
    public void onSaveAccepted(EnrollAcceptedEvent event) {
        enrollNotificationService.saveOnEnrollAccepted(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @EventListener
    public void onSaveRejected(EnrollRejectedEvent event) {
        enrollNotificationService.saveOnEnrollRejected(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @EventListener
    public void onSaveCancelled(EnrollCancelledEvent event) {
        enrollNotificationService.saveOnEnrollCancelled(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchRequested(EnrollRequestedEvent event) {
        enrollNotificationDispatchService.dispatchOnEnrollRequested(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchAccepted(EnrollAcceptedEvent event) {
        enrollNotificationDispatchService.dispatchOnEnrollAccepted(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchRejected(EnrollRejectedEvent event) {
        enrollNotificationDispatchService.dispatchOnEnrollRejected(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @Async("notificationDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchCancelled(EnrollCancelledEvent event) {
        enrollNotificationDispatchService.dispatchOnEnrollCancelled(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }
}
