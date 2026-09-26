package com.back.catchmate.enroll.application.event;

import com.back.catchmate.enroll.application.EnrollCommandService;
import com.back.catchmate.user.domain.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EnrollUserBlockedListener {
    private final EnrollCommandService enrollCommandService;

    // 차단이 확정된 뒤 정리한다. 정리가 실패해도 차단을 롤백할 이유가 없다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserBlockedEvent event) {
        enrollCommandService.deleteAcceptedEnrollsBetween(event.blockerId(), event.blockedId());
    }
}
