package com.back.catchmate.enroll.event;

import com.back.catchmate.enroll.service.EnrollCommandService;
import com.back.catchmate.user.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollUserBlockedEventListener {
    private final EnrollCommandService enrollCommandService;

    @EventListener
    public void handleUserBlockedEvent(UserBlockedEvent event) {
        enrollCommandService.deleteAcceptedEnrollsBetween(event.blockerId(), event.blockedId());
    }
}
