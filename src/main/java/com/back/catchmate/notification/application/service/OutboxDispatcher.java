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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxDispatcher implements OutboxDispatchUseCase {
    private final ObjectMapper objectMapper;
    private final OutboxStateTransitioner outboxStateTransitioner;
    private final NotificationSenderPort notificationSenderPort;
    private final UserOnlineStatusFetchPort userOnlineStatusFetchPort;

    @Value("${notification.outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${notification.outbox.batch-size:50}")
    private int batchSize;

    // FCM 발송(@Retryable 3회 + backoff)이 끝나고도 남을 만큼 넉넉해야 정상 처리 중인 행을 뺏지 않는다.
    @Value("${notification.outbox.processing-timeout-seconds:300}")
    private int processingTimeoutSeconds;

    @Override
    public void sendPendingOutboxImmediately(Long recipientId) {
        log.debug("[아웃박스] sendPendingOutboxImmediately 호출 - 수신자 ID: {}", recipientId);
        List<NotificationOutbox> claimedOutboxes = outboxStateTransitioner.claimPendingByRecipientId(recipientId);
        if (claimedOutboxes.isEmpty()) {
            log.debug("[아웃박스] 수신자 ID {}에 대해 대기 중인(Pending) 아웃박스가 존재하지 않습니다.", recipientId);
            return;
        }
        log.debug("[아웃박스] 수신자 ID {}에 대해 {}건의 대기 중인 아웃박스를 확보했습니다. 발송을 처리합니다.", recipientId, claimedOutboxes.size());
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

    @Override
    public void recoverStuckProcessing() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(processingTimeoutSeconds);
        int recovered = outboxStateTransitioner.recoverStuckProcessing(threshold, maxRetryCount, batchSize);
        if (recovered > 0) {
            log.warn("[아웃박스] PROCESSING 상태로 {}초 이상 정체된 알림 {}건을 회수했습니다.", processingTimeoutSeconds, recovered);
        }
    }

    private void processIndividualNotification(NotificationOutbox outbox) {
        try {
            Map<String, String> payload = parsePayload(outbox.getPayload());
            // 배치 INSERT 시점엔 id 를 알 수 없어(JDBC 멀티로우) DB payload 컬럼 대신 발송 직전에 싣는다.
            payload.put(NotificationSenderPort.DEDUP_KEY, String.valueOf(outbox.getId()));
            String type = payload.get("type");
            if ("CHAT".equals(type)) {
                String roomIdStr = payload.get("roomId");
                if (roomIdStr != null) {
                    Long chatRoomId = Long.parseLong(roomIdStr);
                    Long focusRoomId = userOnlineStatusFetchPort.getUserFocusRoom(outbox.getRecipientId());
                    if (chatRoomId.equals(focusRoomId)) {
                        log.debug("[아웃박스] 수신자 {}가 현재 채팅방 {}을 보고 있으므로 FCM 발송을 생략하고 성공 처리합니다.",
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
            return new HashMap<>();
        }
    }
}
