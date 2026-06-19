package com.back.catchmate.enroll.adapter.in.event;

import com.back.catchmate.enroll.application.port.in.EnrollInternalCommandUseCase;
import com.back.catchmate.user.application.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollUserBlockedEventListener {
    private final EnrollInternalCommandUseCase enrollInternalCommandUseCase;

    @EventListener
    public void handleUserBlockedEvent(UserBlockedEvent event) {
        enrollInternalCommandUseCase.deleteAcceptedEnrollsBetween(event.blockerId(), event.blockedId());
    }
}
