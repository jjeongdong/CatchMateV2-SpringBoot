package com.back.catchmate.notification.application.port.in;

import java.util.Map;

public interface OutboxSaveUseCase {
    void saveOutbox(Long recipientId,
                    String recipientAddress,
                    String title,
                    String body,
                    Map<String, String> data);
}
