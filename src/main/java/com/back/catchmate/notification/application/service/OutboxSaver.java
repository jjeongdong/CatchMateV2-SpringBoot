package com.back.catchmate.notification.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.notification.application.port.in.OutboxRecipient;
import com.back.catchmate.notification.application.port.in.OutboxSaveUseCase;
import com.back.catchmate.notification.application.port.out.persistence.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxSaver implements OutboxSaveUseCase {
    private final NotificationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveOutbox(Long recipientId,
                           String recipientAddress,
                           String title,
                           String body,
                           Map<String, String> data) {
        try {
            String payloadJson = objectMapper.writeValueAsString(data);
            NotificationOutbox outbox = NotificationOutbox.create(recipientId, recipientAddress, title, body, payloadJson);
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("아웃박스 저장 중 에러 발생", e);
            throw new BaseException(ErrorCode.NOTIFICATION_OUTBOX_SAVE_FAILED);
        }
    }

    @Override
    @Transactional
    public void saveOutboxBatch(List<OutboxRecipient> recipients,
                                String title,
                                String body,
                                Map<String, String> data) {
        if (recipients.isEmpty()) {
            return;
        }
        try {
            // payload 는 수신자 전원 공유이므로 1회만 직렬화한다.
            String payloadJson = objectMapper.writeValueAsString(data);
            List<NotificationOutbox> outboxes = recipients.stream()
                    .map(recipient -> NotificationOutbox.create(
                            recipient.recipientId(), recipient.recipientAddress(), title, body, payloadJson))
                    .toList();
            outboxRepository.saveAll(outboxes);
        } catch (Exception e) {
            log.error("아웃박스 배치 저장 중 에러 발생", e);
            throw new BaseException(ErrorCode.NOTIFICATION_OUTBOX_SAVE_FAILED);
        }
    }
}
