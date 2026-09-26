package com.back.catchmate.user.application.event;

import com.back.catchmate.auth.domain.event.LoggedOutEvent;
import com.back.catchmate.user.application.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserLoggedOutListener {
    private final UserCommandService userCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(LoggedOutEvent event) {
        userCommandService.clearFcmToken(event.userId());
    }
}
