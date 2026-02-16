package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.notification.model.NotificationDelivery;
import com.back.catchmate.domain.notification.port.NotificationSenderPort;
import com.back.catchmate.domain.notification.repository.NotificationDeliveryRepository;
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
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationSenderPort notificationSenderPort;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY_COUNT = 5;

    @Transactional
    public void saveFailedNotification(Long recipientId, String token, String title, String body, Map<String, String> data) {
        try {
            String payloadJson = objectMapper.writeValueAsString(data);
            NotificationDelivery delivery = NotificationDelivery.create(recipientId, token, title, body, payloadJson);
            deliveryRepository.save(delivery);
            log.info("푸시 전송 실패 건 DLQ 저장 완료 - recipientId: {}", recipientId);
        } catch (Exception e) {
            log.error("DLQ 저장 중 에러 발생", e);
        }
    }

    @Transactional
    public void retryFailedNotifications() {
        List<NotificationDelivery> pendingDeliveries = deliveryRepository.findAllPending(MAX_RETRY_COUNT);

        if (pendingDeliveries.isEmpty()) return;
        
        log.info("재전송 대상 {}건 발견. 처리를 시작합니다.", pendingDeliveries.size());

        for (NotificationDelivery delivery : pendingDeliveries) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> data = objectMapper.readValue(delivery.getPayload(), Map.class);
                
                notificationSenderPort.sendNotificationIfOffline(
                        delivery.getRecipientId(),
                        delivery.getFcmToken(),
                        delivery.getTitle(),
                        delivery.getBody(),
                        data
                );

                delivery.success();
            } catch (Exception e) {
                delivery.incrementRetryCount();
                if (delivery.getRetryCount() >= MAX_RETRY_COUNT) {
                    delivery.fail();
                }
                log.warn("재전송 실패 - ID: {}, Count: {}", delivery.getId(), delivery.getRetryCount());
            }
            // 도메인 객체의 변경사항을 저장
            deliveryRepository.save(delivery);
        }
    }
}
