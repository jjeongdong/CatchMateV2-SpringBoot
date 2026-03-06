package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.domain.notification.port.NotificationSenderPort;
import com.back.catchmate.domain.notification.repository.NotificationOutboxRepository;
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
public class NotificationRetryService {
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationSenderPort notificationSenderPort;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY_COUNT = 5;

    /**
     * 알림 발송을 위한 아웃박스 데이터를 저장합니다.
     * 트랜잭션 내에서 호출되어야 하며, 초기 상태는 PENDING입니다.
     */
    @Transactional
    public void saveOutbox(Long recipientId, String token, String title, String body, Map<String, String> data) {
        try {
            String payloadJson = objectMapper.writeValueAsString(data);
            NotificationOutbox outbox = NotificationOutbox.create(recipientId, token, title, body, payloadJson);
            outboxRepository.save(outbox);
            log.info("아웃박스 저장 완료 - recipientId: {}", recipientId);
        } catch (Exception e) {
            log.error("아웃박스 저장 중 에러 발생", e);
        }
    }

    /**
     * PENDING 상태인 알림들을 가져와 실제 발송을 시도합니다.
     */
    @Transactional
    public void processPendingNotifications() {
        List<NotificationOutbox> pendingOutboxes = outboxRepository.findAllPending(MAX_RETRY_COUNT);

        if (pendingOutboxes.isEmpty()) return;
        
        log.info("처리 대상 알림 {}건 발견. 발송을 시작합니다.", pendingOutboxes.size());

        for (NotificationOutbox outbox : pendingOutboxes) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> data = objectMapper.readValue(outbox.getPayload(), Map.class);
                
                notificationSenderPort.sendNotificationIfOffline(
                        outbox.getRecipientId(),
                        outbox.getFcmToken(),
                        outbox.getTitle(),
                        outbox.getBody(),
                        data
                );

                outbox.success();
            } catch (Exception e) {
                outbox.incrementRetryCount();
                if (outbox.getRetryCount() >= MAX_RETRY_COUNT) {
                    outbox.fail();
                }
                log.warn("알림 발송 실패 - ID: {}, Count: {}", outbox.getId(), outbox.getRetryCount());
            }
            outboxRepository.save(outbox);
        }
    }
}
