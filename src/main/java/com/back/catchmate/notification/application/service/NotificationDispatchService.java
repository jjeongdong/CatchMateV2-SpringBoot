package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.NotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.out.external.NotificationDispatchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService implements NotificationDispatchUseCase {
    private final NotificationDispatchPort notificationDispatchPort;

    @Override
    public void dispatch(Long userId, Map<String, String> payload) {
        notificationDispatchPort.dispatch(userId, payload);
    }
}
