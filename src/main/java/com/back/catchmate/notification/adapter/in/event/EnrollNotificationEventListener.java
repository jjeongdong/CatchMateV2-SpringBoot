package com.back.catchmate.notification.adapter.in.event;

import com.back.catchmate.enroll.application.event.EnrollAcceptedEvent;
import com.back.catchmate.enroll.application.event.EnrollCancelledEvent;
import com.back.catchmate.enroll.application.event.EnrollRejectedEvent;
import com.back.catchmate.enroll.application.event.EnrollRequestedEvent;
import com.back.catchmate.notification.application.port.in.EnrollNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {
    private final EnrollNotificationUseCase enrollNotificationUseCase;

    @EventListener
    public void onSaveRequested(EnrollRequestedEvent event) {
        enrollNotificationUseCase.saveOnEnrollRequested(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @EventListener
    public void onSaveAccepted(EnrollAcceptedEvent event) {
        enrollNotificationUseCase.saveOnEnrollAccepted(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @EventListener
    public void onSaveRejected(EnrollRejectedEvent event) {
        enrollNotificationUseCase.saveOnEnrollRejected(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @EventListener
    public void onSaveCancelled(EnrollCancelledEvent event) {
        enrollNotificationUseCase.saveOnEnrollCancelled(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchRequested(EnrollRequestedEvent event) {
        enrollNotificationUseCase.dispatchOnEnrollRequested(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchAccepted(EnrollAcceptedEvent event) {
        enrollNotificationUseCase.dispatchOnEnrollAccepted(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchRejected(EnrollRejectedEvent event) {
        enrollNotificationUseCase.dispatchOnEnrollRejected(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchCancelled(EnrollCancelledEvent event) {
        enrollNotificationUseCase.dispatchOnEnrollCancelled(
                event.enrollId(), event.boardId(), event.applicantId(), event.boardOwnerId()
        );
    }
}
