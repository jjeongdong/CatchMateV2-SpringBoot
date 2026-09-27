package com.back.catchmate.notification.application;

import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.OutboxRecipient;
import com.back.catchmate.notification.domain.exception.NotificationOutboxSaveFailedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxWriter {
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void write(Long recipientId, String recipientAddress, String title, String body, Map<String, String> data) {
        notificationOutboxRepository.save(
                NotificationOutbox.create(recipientId, recipientAddress, title, body, toJson(data)));
    }

    @Transactional
    public void writeAll(List<OutboxRecipient> recipients, String title, String body, Map<String, String> data) {
        if (recipients.isEmpty()) {
            return;
        }
        // payload 는 수신자 전원이 공유하므로 한 번만 직렬화한다.
        String payload = toJson(data);
        notificationOutboxRepository.saveAllInBatch(recipients.stream()
                .map(recipient -> NotificationOutbox.create(
                        recipient.recipientId(), recipient.recipientAddress(), title, body, payload))
                .toList());
    }

    private String toJson(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new NotificationOutboxSaveFailedException(e);
        }
    }
}
