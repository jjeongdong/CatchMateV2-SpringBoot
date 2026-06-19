package com.back.catchmate.notification.application.port.in;

import java.util.Map;

public interface NotificationDispatchUseCase {
    void dispatch(Long userId, Map<String, String> payload);
}
