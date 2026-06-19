package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import com.back.catchmate.notification.application.port.out.external.NotificationSenderPort;
import com.back.catchmate.notification.application.port.out.exception.PermanentNotificationFailureException;
import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.back.catchmate.notification.application.port.out.external.UserOnlineStatusFetchPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxDispatcher implements OutboxDispatchUseCase {
    private final NotificationSenderPort notificationSenderPort;
    private final OutboxStateTransitioner outboxStateTransitioner;
    private final ObjectMapper objectMapper;
    private final UserOnlineStatusFetchPort userOnlineStatusFetchPort;

    @Value("${notification.outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${notification.outbox.batch-size:50}")
    private int batchSize;

    @Override
    public void sendPendingOutboxImmediately(Long recipientId) {
        log.info("[아웃박스] sendPendingOutboxImmediately 호출 - 수신자 ID: {}", recipientId);
        List<NotificationOutbox> claimedOutboxes = outboxStateTransitioner.claimPendingByRecipientId(recipientId);
        if (claimedOutboxes.isEmpty()) {
            log.info("[아웃박스] 수신자 ID {}에 대해 대기 중인(Pending) 아웃박스가 존재하지 않습니다.", recipientId);
            return;
        }
        log.info("[아웃박스] 수신자 ID {}에 대해 {}건의 대기 중인 아웃박스를 확보했습니다. 발송을 처리합니다.", recipientId, claimedOutboxes.size());
        for (NotificationOutbox outbox : claimedOutboxes) {
            processIndividualNotification(outbox);
        }
    }

    @Override
    public void processPendingNotifications() {
        List<NotificationOutbox> claimList = outboxStateTransitioner.claimPendingNotifications(maxRetryCount, batchSize);
        if (claimList.isEmpty()) return;

        log.info("처리 대상 알림 {}건을 선점했습니다. 발송을 시작합니다.", claimList.size());

        for (NotificationOutbox outbox : claimList) {
            processIndividualNotification(outbox);
        }
    }

    private void processIndividualNotification(NotificationOutbox outbox) {
        try {
            Map<String, String> payload = parsePayload(outbox.getPayload());
            String type = payload.get("type");
            if ("CHAT".equals(type)) {
                String roomIdStr = payload.get("roomId");
                if (roomIdStr != null) {
                    Long chatRoomId = Long.parseLong(roomIdStr);
                    Long focusRoomId = userOnlineStatusFetchPort.getUserFocusRoom(outbox.getRecipientId());
                    if (chatRoomId.equals(focusRoomId)) {
                        log.info("[아웃박스] 수신자 {}가 현재 채팅방 {}을 보고 있으므로 FCM 발송을 생략하고 성공 처리합니다.",
                                outbox.getRecipientId(), chatRoomId);
                        outboxStateTransitioner.updateStatusSuccess(outbox);
                        return;
                    }
                }
            }

            notificationSenderPort.sendNotification(
                    outbox.getRecipientId(),
                    outbox.getRecipientAddress(),
                    outbox.getTitle(),
                    outbox.getBody(),
                    payload
            );
            outboxStateTransitioner.updateStatusSuccess(outbox);
        } catch (PermanentNotificationFailureException e) {
            log.warn("알림 영구 실패 (ID: {}) - 재시도 중단. 사유: {}", outbox.getId(), e.getMessage());
            outboxStateTransitioner.updateStatusPermanentFailure(outbox, e.getMessage());
        } catch (Exception e) {
            log.warn("알림 발송 실패 (ID: {}) - 재시도 카운트 증가. 사유: {}", outbox.getId(), e.getMessage());
            outboxStateTransitioner.updateStatusFailure(outbox, maxRetryCount, e.getMessage());
        }
    }

    private Map<String, String> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("페이로드 파싱 실패 - 빈 Map으로 대체: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
