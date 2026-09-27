package com.back.catchmate.notification.application;

import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.PushMessage;
import com.back.catchmate.notification.domain.PushOutcome;
import com.back.catchmate.notification.domain.PushSender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// 트랜잭션 없이 돈다. 상태 전이는 OutboxStateTransitioner 의 짧은 트랜잭션에 맡기고, FCM 호출 동안 커넥션을 잡지 않는다.
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxDispatcher {
    private final ObjectMapper objectMapper;
    private final OutboxStateTransitioner outboxStateTransitioner;
    private final PushSender pushSender;
    private final ChatQueryApi chatQueryApi;

    @Value("${notification.outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${notification.outbox.batch-size:500}")
    private int batchSize;

    // 배치 발송(FCM 왕복 + 상태 확정)이 끝나고도 남을 만큼 넉넉해야 정상 처리 중인 행을 뺏지 않는다.
    @Value("${notification.outbox.processing-timeout-seconds:300}")
    private int processingTimeoutSeconds;

    /**
     * 수신자 1명분의 대기 알림을 즉시 발송한다(신청 등 수신자가 1~2명인 알림 전용).
     * 대량 브로드캐스트에 쓰면 수신자 수만큼 이 경로가 순차 반복되므로 사용하지 않는다.
     */
    public void sendPendingOutboxImmediately(Long recipientId) {
        List<NotificationOutbox> claimed = outboxStateTransitioner.claimPendingByRecipientId(recipientId);
        for (NotificationOutbox outbox : claimed) {
            sendOne(outbox);
        }
    }

    public void processPendingNotifications() {
        List<NotificationOutbox> claimed = outboxStateTransitioner.claimPendingNotifications(maxRetryCount, batchSize);
        if (claimed.isEmpty()) {
            return;
        }
        log.info("아웃박스 배치 발송 시작 count={}", claimed.size());
        try {
            sendBatch(claimed);
        } catch (Exception e) {
            // 배치 도중 예기치 못한 실패(예: 포커스 방 조회 중 Redis 장애)가 나면 선점한 행 전체가
            // PROCESSING 에 갇힌다. recoverStuckProcessing 이 회수할 때까지 수분간 멈추므로 여기서 되돌린다.
            // 이미 발송된 건이 섞여 있을 수 있으나, 재발송은 dedupKey 로 수신 측에서 걸러진다(at-least-once).
            log.error("아웃박스 배치 처리 실패, 선점한 행을 재시도 대상으로 되돌림 count={}", claimed.size(), e);
            rollbackClaimToRetryable(claimed, e);
        }
    }

    public void recoverStuckProcessing() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(processingTimeoutSeconds);
        int recovered = outboxStateTransitioner.recoverStuckProcessing(threshold, maxRetryCount, batchSize);
        if (recovered > 0) {
            log.warn("PROCESSING 정체 아웃박스 회수 count={}, timeoutSeconds={}", recovered, processingTimeoutSeconds);
        }
    }

    private void rollbackClaimToRetryable(List<NotificationOutbox> claimed, Exception cause) {
        String reason = "배치 처리 실패 - " + cause.getMessage();
        Map<Long, String> errorMessages = new HashMap<>();
        for (NotificationOutbox outbox : claimed) {
            errorMessages.put(outbox.getId(), reason);
        }
        outboxStateTransitioner.applyDispatchResults(List.of(), List.of(), claimed, errorMessages, maxRetryCount);
    }

    /**
     * 선점한 배치를 한 번에 처리한다.
     * <p>
     * 건별로 (Redis 포커스 조회 → FCM 블로킹 발송 → 상태 트랜잭션) 을 반복하면 배치 크기에 비례해 지연이 쌓인다.
     * 그래서 포커스 방은 MGET 한 번, FCM 은 배치 발송 한 번, 상태 확정은 트랜잭션 한 번으로 묶는다.
     */
    private void sendBatch(List<NotificationOutbox> claimed) {
        Map<Long, Map<String, String>> payloadsById = new HashMap<>();
        for (NotificationOutbox outbox : claimed) {
            payloadsById.put(outbox.getId(), payloadOf(outbox));
        }
        Map<Long, Long> focusRoomsByUserId = fetchChatFocusRooms(claimed, payloadsById);

        List<NotificationOutbox> successes = new ArrayList<>();
        List<NotificationOutbox> targets = new ArrayList<>();
        List<PushMessage> messages = new ArrayList<>();
        for (NotificationOutbox outbox : claimed) {
            Map<String, String> payload = payloadsById.get(outbox.getId());
            if (isViewingChatRoom(payload, focusRoomsByUserId.get(outbox.getRecipientId()))) {
                successes.add(outbox);
                continue;
            }
            targets.add(outbox);
            messages.add(toPushMessage(outbox, payload));
        }

        List<PushOutcome> results = pushSender.sendAll(messages);

        List<NotificationOutbox> permanentFailures = new ArrayList<>();
        List<NotificationOutbox> retryableFailures = new ArrayList<>();
        Map<Long, String> errorMessages = new HashMap<>();
        for (int i = 0; i < targets.size(); i++) {
            NotificationOutbox outbox = targets.get(i);
            PushOutcome result = results.get(i);
            if (result.success()) {
                successes.add(outbox);
                continue;
            }
            errorMessages.put(outbox.getId(), result.errorMessage());
            if (result.permanentFailure()) {
                permanentFailures.add(outbox);
            } else {
                retryableFailures.add(outbox);
            }
        }

        outboxStateTransitioner.applyDispatchResults(
                successes, permanentFailures, retryableFailures, errorMessages, maxRetryCount);
    }

    // 포커스 방 확인이 필요한 건 채팅 알림뿐이다. 수신자별 왕복 대신 MGET 한 번으로 모아온다.
    private Map<Long, Long> fetchChatFocusRooms(
            List<NotificationOutbox> claimed, Map<Long, Map<String, String>> payloadsById) {
        List<Long> chatRecipientIds = claimed.stream()
                .filter(outbox -> NotificationPayload.isChat(payloadsById.get(outbox.getId())))
                .map(NotificationOutbox::getRecipientId)
                .distinct()
                .toList();
        if (chatRecipientIds.isEmpty()) {
            return Map.of();
        }
        return chatQueryApi.getFocusRooms(chatRecipientIds);
    }

    private void sendOne(NotificationOutbox outbox) {
        try {
            Map<String, String> payload = payloadOf(outbox);
            if (NotificationPayload.isChat(payload)
                    && isViewingChatRoom(
                            payload,
                            chatQueryApi.findFocusRoom(outbox.getRecipientId()).orElse(null))) {
                outboxStateTransitioner.updateStatusSuccess(outbox);
                return;
            }
            PushOutcome result = pushSender.send(toPushMessage(outbox, payload));
            if (result.success()) {
                outboxStateTransitioner.updateStatusSuccess(outbox);
            } else if (result.permanentFailure()) {
                outboxStateTransitioner.updateStatusPermanentFailure(outbox, result.errorMessage());
            } else {
                outboxStateTransitioner.updateStatusFailure(outbox, maxRetryCount, result.errorMessage());
            }
        } catch (Exception e) {
            // 푸시 전 단계(포커스 방 조회 등)의 장애도 재시도 대상으로 돌려 PROCESSING 에 갇히지 않게 한다.
            log.warn("아웃박스 즉시 발송 실패, 재시도 대상으로 되돌림 outboxId={}", outbox.getId(), e);
            outboxStateTransitioner.updateStatusFailure(outbox, maxRetryCount, e.getMessage());
        }
    }

    // 수신자가 그 채팅방을 보고 있으면 푸시가 불필요하다 (화면에 이미 메시지가 보인다).
    private boolean isViewingChatRoom(Map<String, String> payload, Long focusRoomId) {
        if (focusRoomId == null || !NotificationPayload.isChat(payload)) {
            return false;
        }
        return focusRoomId.equals(parseRoomId(payload.get(NotificationPayload.ROOM_ID)));
    }

    // 한 행의 roomId 가 깨져 있어도 배치 전체가 멈추지 않도록, 파싱 실패는 '포커스 아님'으로 흘려보낸다.
    private Long parseRoomId(String roomId) {
        if (roomId == null) {
            return null;
        }
        try {
            return Long.parseLong(roomId);
        } catch (NumberFormatException e) {
            log.warn("아웃박스 roomId 파싱 실패 roomId={}", roomId);
            return null;
        }
    }

    private PushMessage toPushMessage(NotificationOutbox outbox, Map<String, String> payload) {
        return new PushMessage(
                outbox.getRecipientId(), outbox.getRecipientAddress(), outbox.getTitle(), outbox.getBody(), payload);
    }

    // 배치 INSERT 시점엔 id 를 알 수 없어(JDBC 멀티로우) dedupKey 를 DB payload 대신 발송 직전에 싣는다.
    private Map<String, String> payloadOf(NotificationOutbox outbox) {
        Map<String, String> payload = parsePayload(outbox.getPayload());
        payload.put(NotificationPayload.DEDUP_KEY, String.valueOf(outbox.getId()));
        return payload;
    }

    private Map<String, String> parsePayload(String payload) {
        try {
            return new HashMap<>(objectMapper.readValue(payload, new TypeReference<Map<String, String>>() {}));
        } catch (Exception e) {
            log.warn("아웃박스 payload 파싱 실패, 빈 데이터로 발송 reason={}", e.getMessage());
            return new HashMap<>();
        }
    }
}
