package com.back.catchmate.notification.application.port.in;

import java.util.List;
import java.util.Map;

public interface OutboxSaveUseCase {
    void saveOutbox(Long recipientId,
                    String recipientAddress,
                    String title,
                    String body,
                    Map<String, String> data);

    // 공유 title/body/data 로 여러 수신자의 Outbox 를 한 번에 저장한다(멀티로우 INSERT).
    void saveOutboxBatch(List<OutboxRecipient> recipients,
                         String title,
                         String body,
                         Map<String, String> data);
}
