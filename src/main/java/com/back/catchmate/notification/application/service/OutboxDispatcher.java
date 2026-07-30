package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationMessage;
import com.back.catchmate.notification.application.port.out.dto.NotificationSendResult;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxDispatcher implements OutboxDispatchUseCase {
    private static final String CHAT_TYPE = "CHAT";

    private final ObjectMapper objectMapper;
    private final OutboxStateTransitioner outboxStateTransitioner;
    private final NotificationSenderPort notificationSenderPort;
    private final UserOnlineStatusFetchPort userOnlineStatusFetchPort;

    @Value("${notification.outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${notification.outbox.batch-size:500}")
    private int batchSize;

    // 배치 발송(FCM 왕복 + 상태 확정)이 끝나고도 남을 만큼 넉넉해야 정상 처리 중인 행을 뺏지 않는다.
    @Value("${notification.outbox.processing-timeout-seconds:300}")
    private int processingTimeoutSeconds;

    /**
     * 수신자 1명분의 대기 알림을 즉시 발송한다(Enroll 등 수신자가 1~2명인 알림 전용).
     * 대량 브로드캐스트에 쓰면 수신자 수만큼 이 경로가 순차 반복되므로 사용하지 않는다.
     */
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
        try {
            processBatch(claimList);
        } catch (Exception e) {
            // 배치 도중 예기치 못한 실패(예: 포커스 방 조회 중 Redis 장애)가 나면 선점한 행 전체가
            // PROCESSING 에 갇힌다. recoverStuckProcessing 이 회수할 때까지 수분간 멈추므로 여기서 되돌린다.
            // 이미 발송된 건이 섞여 있을 수 있으나, 재발송은 dedupKey 로 수신 측에서 걸러진다(at-least-once).
            log.error("[아웃박스] 배치 처리 실패 - 선점한 {}건을 재시도 대상으로 되돌립니다.", claimList.size(), e);
            rollbackClaimToRetryable(claimList, e);
        }
    }

    private void rollbackClaimToRetryable(List<NotificationOutbox> claimList, Exception cause) {
        String reason = "배치 처리 실패 - " + cause.getMessage();
        Map<Long, String> errorMessages = claimList.stream()
                .collect(Collectors.toMap(NotificationOutbox::getId, outbox -> reason));
        outboxStateTransitioner.applyDispatchResults(
                List.of(), List.of(), claimList, errorMessages, maxRetryCount);
    }

    @Override
    public void recoverStuckProcessing() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(processingTimeoutSeconds);
        int recovered = outboxStateTransitioner.recoverStuckProcessing(threshold, maxRetryCount, batchSize);
        if (recovered > 0) {
            log.warn("[아웃박스] PROCESSING 상태로 {}초 이상 정체된 알림 {}건을 회수했습니다.", processingTimeoutSeconds, recovered);
        }
    }

    /**
     * 선점한 배치를 한 번에 처리한다.
     * <p>
     * 건별로 (Redis 포커스 조회 → FCM 블로킹 발송 → 상태 트랜잭션) 을 반복하면 배치 크기에 비례해 지연이 쌓인다.
     * 그래서 포커스 방은 MGET 한 번, FCM 은 배치 발송 한 번, 상태 확정은 트랜잭션 한 번으로 묶는다.
     */
    private void processBatch(List<NotificationOutbox> claimList) {
        Map<Long, Map<String, String>> payloads = new HashMap<>();
        for (NotificationOutbox outbox : claimList) {
            Map<String, String> payload = parsePayload(outbox.getPayload());
            // 배치 INSERT 시점엔 id 를 알 수 없어(JDBC 멀티로우) DB payload 컬럼 대신 발송 직전에 싣는다.
            payload.put(NotificationSenderPort.DEDUP_KEY, String.valueOf(outbox.getId()));
            payloads.put(outbox.getId(), payload);
        }

        Map<Long, Long> focusRooms = fetchChatFocusRooms(claimList, payloads);

        List<NotificationOutbox> successes = new ArrayList<>();
        List<NotificationOutbox> targets = new ArrayList<>();
        List<NotificationMessage> messages = new ArrayList<>();
        for (NotificationOutbox outbox : claimList) {
            Map<String, String> payload = payloads.get(outbox.getId());
            if (isRecipientViewingChatRoom(outbox, payload, focusRooms)) {
                successes.add(outbox);
                continue;
            }
            targets.add(outbox);
            messages.add(new NotificationMessage(
                    outbox.getRecipientId(),
                    outbox.getRecipientAddress(),
                    outbox.getTitle(),
                    outbox.getBody(),
                    payload
            ));
        }

        List<NotificationSendResult> results = notificationSenderPort.sendNotifications(messages);

        List<NotificationOutbox> permanentFailures = new ArrayList<>();
        List<NotificationOutbox> retryableFailures = new ArrayList<>();
        Map<Long, String> errorMessages = new HashMap<>();
        for (int i = 0; i < targets.size(); i++) {
            NotificationOutbox outbox = targets.get(i);
            NotificationSendResult result = results.get(i);
            if (result.success()) {
                successes.add(outbox);
                continue;
            }
            errorMessages.put(outbox.getId(), result.errorMessage());
            if (result.permanentFailure()) {
                log.warn("알림 영구 실패 (ID: {}) - 재시도 중단. 사유: {}", outbox.getId(), result.errorMessage());
                permanentFailures.add(outbox);
            } else {
                log.warn("알림 발송 실패 (ID: {}) - 재시도 카운트 증가. 사유: {}", outbox.getId(), result.errorMessage());
                retryableFailures.add(outbox);
            }
        }

        outboxStateTransitioner.applyDispatchResults(
                successes, permanentFailures, retryableFailures, errorMessages, maxRetryCount);
    }

    // 포커스 방 확인이 필요한 건 CHAT 알림뿐이다. 수신자별 왕복 대신 MGET 한 번으로 모아온다.
    private Map<Long, Long> fetchChatFocusRooms(List<NotificationOutbox> claimList, Map<Long, Map<String, String>> payloads) {
        List<Long> chatRecipientIds = claimList.stream()
                .filter(outbox -> isChat(payloads.get(outbox.getId())))
                .map(NotificationOutbox::getRecipientId)
                .distinct()
                .toList();
        if (chatRecipientIds.isEmpty()) {
            return Map.of();
        }
        return userOnlineStatusFetchPort.getUserFocusRooms(chatRecipientIds);
    }

    private boolean isRecipientViewingChatRoom(NotificationOutbox outbox, Map<String, String> payload, Map<Long, Long> focusRooms) {
        if (!isChat(payload)) return false;

        Long chatRoomId = parseRoomId(payload.get("roomId"));
        if (chatRoomId == null || !chatRoomId.equals(focusRooms.get(outbox.getRecipientId()))) {
            return false;
        }
        log.debug("[아웃박스] 수신자 {}가 현재 채팅방 {}을 보고 있으므로 FCM 발송을 생략하고 성공 처리합니다.",
                outbox.getRecipientId(), chatRoomId);
        return true;
    }

    private boolean isChat(Map<String, String> payload) {
        return CHAT_TYPE.equals(payload.get("type"));
    }

    // 한 행의 roomId 가 깨져 있어도 배치 전체가 중단되지 않도록, 파싱 실패는 '포커스 아님'으로 흘려보낸다.
    private Long parseRoomId(String roomIdStr) {
        if (roomIdStr == null) return null;
        try {
            return Long.parseLong(roomIdStr);
        } catch (NumberFormatException e) {
            log.warn("[아웃박스] roomId 파싱 실패 - 값: {}", roomIdStr);
            return null;
        }
    }

    private void processIndividualNotification(NotificationOutbox outbox) {
        try {
            Map<String, String> payload = parsePayload(outbox.getPayload());
            // 배치 INSERT 시점엔 id 를 알 수 없어(JDBC 멀티로우) DB payload 컬럼 대신 발송 직전에 싣는다.
            payload.put(NotificationSenderPort.DEDUP_KEY, String.valueOf(outbox.getId()));
            if (isChat(payload)) {
                Long chatRoomId = parseRoomId(payload.get("roomId"));
                if (chatRoomId != null) {
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
