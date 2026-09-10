package com.back.catchmate.notification.service;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import com.back.catchmate.notification.entity.enums.NotificationTemplate;
import com.back.catchmate.notification.infra.RedisNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Enroll 알림의 비동기 발송(STOMP 실시간 + FCM 즉시 시도) 전용 서비스.
 * <p>
 * 의도적으로 클래스 레벨 {@code @Transactional} 을 두지 않는다. 발송 경로는 직접 엔티티 쓰기가 없고
 * (Outbox claim/update 는 {@code OutboxStateTransitioner} 의 REQUIRES_NEW 짧은 트랜잭션으로 처리),
 * 외부 FCM 호출(수백 ms) 동안 DB 커넥션을 점유하지 않기 위함이다.
 * 영속화(쓰기)는 {@link EnrollNotificationService}(@Transactional) 가 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollNotificationDispatchService {
    private static final String TYPE_ENROLL_REQUEST = "ENROLL_REQUEST";
    private static final String TYPE_ENROLL_ACCEPTED = "ENROLL_ACCEPTED";
    private static final String TYPE_ENROLL_REJECTED = "ENROLL_REJECTED";
    private static final String TYPE_ENROLL_CANCEL = "ENROLL_CANCEL";

    private final BoardService boardService;
    private final UserService userService;
    private final OutboxDispatcher outboxDispatcher;
    private final RedisNotificationPublisher redisNotificationPublisher;

    public void dispatchOnEnrollRequested(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] dispatchOnEnrollRequested 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        UserSummary recipient = userService.getUserSummary(boardOwnerId);
        if (!recipient.enrollAlarmEnabled()) {
            log.warn("[Enroll알림] 수신자(boardOwnerId: {})의 enrollAlarm 설정이 비활성화(false)되어 발송을 중단합니다.", boardOwnerId);
            return;
        }

        UserSummary applicant = userService.getUserSummary(applicantId);
        BoardSummary board = boardService.getBoardSummary(boardId);
        String title = NotificationTemplate.ENROLL_REQUEST.formatTitle(applicant.nickName());
        String body = NotificationTemplate.ENROLL_REQUEST.formatBody(board.title());

        dispatch(recipient, boardId, title, body, TYPE_ENROLL_REQUEST, true);
    }

    public void dispatchOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] dispatchOnEnrollAccepted 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        UserSummary recipient = userService.getUserSummary(applicantId);
        if (!recipient.enrollAlarmEnabled()) {
            log.warn("[Enroll알림] 수신자(applicantId: {})의 enrollAlarm 설정이 비활성화(false)되어 발송을 중단합니다.", applicantId);
            return;
        }

        BoardSummary board = boardService.getBoardSummary(boardId);
        String title = NotificationTemplate.ENROLL_ACCEPT.getTitle();
        String body = NotificationTemplate.ENROLL_ACCEPT.formatBody(board.title());

        dispatch(recipient, boardId, title, body, TYPE_ENROLL_ACCEPTED, true);
    }

    public void dispatchOnEnrollRejected(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] dispatchOnEnrollRejected 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        UserSummary recipient = userService.getUserSummary(applicantId);
        if (!recipient.enrollAlarmEnabled()) {
            log.warn("[Enroll알림] 수신자(applicantId: {})의 enrollAlarm 설정이 비활성화(false)되어 발송을 중단합니다.", applicantId);
            return;
        }

        BoardSummary board = boardService.getBoardSummary(boardId);
        String title = NotificationTemplate.ENROLL_REJECT.getTitle();
        String body = NotificationTemplate.ENROLL_REJECT.formatBody(board.title());

        dispatch(recipient, boardId, title, body, TYPE_ENROLL_REJECTED, true);
    }

    public void dispatchOnEnrollCancelled(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] dispatchOnEnrollCancelled 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        UserSummary recipient = userService.getUserSummary(boardOwnerId);
        if (!recipient.enrollAlarmEnabled()) {
            log.warn("[Enroll알림] 수신자(boardOwnerId: {})의 enrollAlarm 설정이 비활성화(false)되어 발송을 중단합니다.", boardOwnerId);
            return;
        }

        UserSummary applicant = userService.getUserSummary(applicantId);
        BoardSummary board = boardService.getBoardSummary(boardId);
        String title = NotificationTemplate.ENROLL_CANCEL.formatTitle(applicant.nickName());
        String body = NotificationTemplate.ENROLL_CANCEL.formatBody(board.title());

        dispatch(recipient, boardId, title, body, TYPE_ENROLL_CANCEL, false);
    }

    private void dispatch(
            UserSummary recipient,
            Long boardId,
            String title,
            String body,
            String type,
            boolean pushEnabled
    ) {
        Map<String, String> payload = createNotificationData(type, boardId, title, body);
        log.info("[Enroll알림] STOMP 알림 전송 시도 - recipientId: {}, payload: {}", recipient.userId(), payload);
        redisNotificationPublisher.dispatch(recipient.userId(), payload);

        if (pushEnabled) {
            log.info("[Enroll알림] FCM 알림 즉시 발송 시도 - recipientId: {}", recipient.userId());
            outboxDispatcher.sendPendingOutboxImmediately(recipient.userId());
        } else {
            log.info("[Enroll알림] FCM 알림 발송 생략 (pushEnabled: false)");
        }
    }

    private Map<String, String> createNotificationData(String type, Long boardId, String title, String body) {
        return Map.of(
                "type", type,
                "boardId", String.valueOf(boardId),
                "title", title,
                "body", body
        );
    }
}
