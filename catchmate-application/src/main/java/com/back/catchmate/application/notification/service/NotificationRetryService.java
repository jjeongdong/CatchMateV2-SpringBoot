package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.domain.notification.port.NotificationSenderPort;
import com.back.catchmate.domain.notification.repository.NotificationOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRetryService {
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationOutboxUpdater outboxUpdater; // 1. 업데이터 주입
    private final NotificationSenderPort notificationSenderPort;
    private final ObjectMapper objectMapper;

    @Value("${notification.outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Transactional
    public void saveOutbox(Long recipientId, String token, String title, String body, Map<String, String> data) {
        try {
            String payloadJson = objectMapper.writeValueAsString(data);
            NotificationOutbox outbox = NotificationOutbox.create(recipientId, token, title, body, payloadJson);
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("아웃박스 저장 중 에러 발생", e);
        }
    }

    public void sendPendingOutboxImmediately(Long recipientId) {
        List<NotificationOutbox> pendingOutboxes = outboxRepository.findAllPendingByRecipientId(recipientId);
        for (NotificationOutbox outbox : pendingOutboxes) {
            processIndividualNotification(outbox);
        }
    }

    public void processPendingNotifications() {
        // 1. 데이터 선점 (트랜잭션 내에서 처리하여 다른 서버가 못 가져가게 함)
        List<NotificationOutbox> claimList = outboxUpdater.claimPendingNotifications(maxRetryCount);

        if (claimList.isEmpty()) return;
        
        log.info("처리 대상 알림 {}건을 선점했습니다. 발송을 시작합니다.", claimList.size());

        for (NotificationOutbox outbox : claimList) {
            processIndividualNotification(outbox);
        }
    }

    private void processIndividualNotification(NotificationOutbox outbox) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> data = objectMapper.readValue(outbox.getPayload(), Map.class);
            
            // 2. 실제 전송 (트랜잭션 밖에서 실행하여 커넥션 고갈 방지)
            notificationSenderPort.sendNotificationIfOffline(
                    outbox.getRecipientId(),
                    outbox.getFcmToken(),
                    outbox.getTitle(),
                    outbox.getBody(),
                    data
            );

            // 3. 성공 처리 (SUCCESS로 업데이트)
            outboxUpdater.updateStatusSuccess(outbox);
        } catch (Exception e) {
            log.warn("알림 발송 실패 (ID: {}) - 재시도 카운트 증가", outbox.getId());
            // 4. 실패 처리 (재시도 횟수 초과 시 FAILED, 아니면 다시 PENDING으로 돌려보낼지 결정)
            outboxUpdater.updateStatusFailure(outbox, maxRetryCount);
        }
    }
}
