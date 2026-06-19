package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.EnrollNotificationUseCase;
import com.back.catchmate.notification.application.port.in.NotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.NotificationInternalCommandUseCase;
import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import com.back.catchmate.notification.application.port.in.OutboxSaveUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationBoardInfo;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.BoardFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.domain.model.AlarmType;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EnrollNotificationService implements EnrollNotificationUseCase {
    private static final String TYPE_ENROLL_REQUEST = "ENROLL_REQUEST";
    private static final String TYPE_ENROLL_ACCEPTED = "ENROLL_ACCEPTED";
    private static final String TYPE_ENROLL_REJECTED = "ENROLL_REJECTED";
    private static final String TYPE_ENROLL_CANCEL = "ENROLL_CANCEL";

    private final UserFetchPort userFetchPort;
    private final BoardFetchPort boardFetchPort;
    private final OutboxSaveUseCase outboxSaveUseCase;
    private final OutboxDispatchUseCase outboxDispatchUseCase;
    private final NotificationDispatchUseCase notificationDispatchUseCase;
    private final NotificationInternalCommandUseCase notificationInternalCommandUseCase;

    @Override
    public void saveOnEnrollRequested(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] saveOnEnrollRequested 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationUserInfo applicant = userFetchPort.getUser(applicantId);
        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        String title = NotificationTemplate.ENROLL_REQUEST.formatTitle(applicant.nickName());
        String body = NotificationTemplate.ENROLL_REQUEST.formatBody(board.title());

        saveNotificationAndOutbox(boardOwnerId, applicantId, boardId, enrollId, title, body, TYPE_ENROLL_REQUEST, true);
    }

    @Override
    public void dispatchOnEnrollRequested(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] dispatchOnEnrollRequested 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationUserInfo recipient = userFetchPort.getUser(boardOwnerId);
        if (!recipient.enrollAlarmEnabled()) {
            log.warn("[Enroll알림] 수신자(boardOwnerId: {})의 enrollAlarm 설정이 비활성화(false)되어 발송을 중단합니다.", boardOwnerId);
            return;
        }

        NotificationUserInfo applicant = userFetchPort.getUser(applicantId);
        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        String title = NotificationTemplate.ENROLL_REQUEST.formatTitle(applicant.nickName());
        String body = NotificationTemplate.ENROLL_REQUEST.formatBody(board.title());

        dispatch(recipient, boardId, title, body, TYPE_ENROLL_REQUEST, true);
    }

    @Override
    public void saveOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] saveOnEnrollAccepted 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        String title = NotificationTemplate.ENROLL_ACCEPT.getTitle();
        String body = NotificationTemplate.ENROLL_ACCEPT.formatBody(board.title());

        saveNotificationAndOutbox(applicantId, boardOwnerId, boardId, enrollId, title, body, TYPE_ENROLL_ACCEPTED, true);
    }

    @Override
    public void dispatchOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] dispatchOnEnrollAccepted 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationUserInfo recipient = userFetchPort.getUser(applicantId);
        if (!recipient.enrollAlarmEnabled()) {
            log.warn("[Enroll알림] 수신자(applicantId: {})의 enrollAlarm 설정이 비활성화(false)되어 발송을 중단합니다.", applicantId);
            return;
        }

        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        String title = NotificationTemplate.ENROLL_ACCEPT.getTitle();
        String body = NotificationTemplate.ENROLL_ACCEPT.formatBody(board.title());

        dispatch(recipient, boardId, title, body, TYPE_ENROLL_ACCEPTED, true);
    }

    @Override
    public void saveOnEnrollRejected(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] saveOnEnrollRejected 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        String title = NotificationTemplate.ENROLL_REJECT.getTitle();
        String body = NotificationTemplate.ENROLL_REJECT.formatBody(board.title());

        saveNotificationAndOutbox(applicantId, boardOwnerId, boardId, enrollId, title, body, TYPE_ENROLL_REJECTED, true);
    }

    @Override
    public void dispatchOnEnrollRejected(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] dispatchOnEnrollRejected 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationUserInfo recipient = userFetchPort.getUser(applicantId);
        if (!recipient.enrollAlarmEnabled()) {
            log.warn("[Enroll알림] 수신자(applicantId: {})의 enrollAlarm 설정이 비활성화(false)되어 발송을 중단합니다.", applicantId);
            return;
        }

        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        String title = NotificationTemplate.ENROLL_REJECT.getTitle();
        String body = NotificationTemplate.ENROLL_REJECT.formatBody(board.title());

        dispatch(recipient, boardId, title, body, TYPE_ENROLL_REJECTED, true);
    }

    @Override
    public void saveOnEnrollCancelled(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] saveOnEnrollCancelled 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationUserInfo applicant = userFetchPort.getUser(applicantId);
        String title = NotificationTemplate.ENROLL_CANCEL.formatTitle(applicant.nickName());

        notificationInternalCommandUseCase.createNotification(
                boardOwnerId,
                applicantId,
                boardId,
                title,
                AlarmType.ENROLL,
                enrollId
        );
    }

    @Override
    public void dispatchOnEnrollCancelled(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] dispatchOnEnrollCancelled 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationUserInfo recipient = userFetchPort.getUser(boardOwnerId);
        if (!recipient.enrollAlarmEnabled()) {
            log.warn("[Enroll알림] 수신자(boardOwnerId: {})의 enrollAlarm 설정이 비활성화(false)되어 발송을 중단합니다.", boardOwnerId);
            return;
        }

        NotificationUserInfo applicant = userFetchPort.getUser(applicantId);
        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        String title = NotificationTemplate.ENROLL_CANCEL.formatTitle(applicant.nickName());
        String body = NotificationTemplate.ENROLL_CANCEL.formatBody(board.title());

        dispatch(recipient, boardId, title, body, TYPE_ENROLL_CANCEL, false);
    }

    private void saveNotificationAndOutbox(
            Long recipientId,
            Long senderId,
            Long boardId,
            Long referenceId,
            String title,
            String body,
            String type,
            boolean pushEnabled
    ) {
        log.info("[Enroll알림] saveNotificationAndOutbox 호출됨 - recipientId: {}, pushEnabled: {}", recipientId, pushEnabled);
        notificationInternalCommandUseCase.createNotification(
                recipientId,
                senderId,
                boardId,
                title,
                AlarmType.ENROLL,
                referenceId
        );

        if (!pushEnabled) {
            log.info("[Enroll알림] pushEnabled가 false이므로 outbox 저장을 생략합니다.");
            return;
        }

        NotificationUserInfo recipient = userFetchPort.getUser(recipientId);
        log.info("[Enroll알림] 수신자 설정 상태 - enrollAlarmEnabled: {}, fcmToken: {}",
                recipient.enrollAlarmEnabled(), recipient.fcmToken());

        if (recipient.enrollAlarmEnabled() && recipient.fcmToken() != null) {
            outboxSaveUseCase.saveOutbox(
                    recipient.userId(),
                    recipient.fcmToken(),
                    title,
                    body,
                    createNotificationData(type, boardId, title, body)
            );
            log.info("[Enroll알림] outbox 테이블에 알림 데이터 저장 완료 (recipientId: {})", recipient.userId());
        } else {
            log.warn("[Enroll알림] outbox 저장 불필요 혹은 조건 미달 (alarmEnabled: {}, token존재여부: {})",
                    recipient.enrollAlarmEnabled(), recipient.fcmToken() != null);
        }
    }

    private void dispatch(
            NotificationUserInfo recipient,
            Long boardId,
            String title,
            String body,
            String type,
            boolean pushEnabled
    ) {
        Map<String, String> payload = createNotificationData(type, boardId, title, body);
        log.info("[Enroll알림] STOMP 알림 전송 시도 - recipientId: {}, payload: {}", recipient.userId(), payload);
        notificationDispatchUseCase.dispatch(recipient.userId(), payload);

        if (pushEnabled) {
            log.info("[Enroll알림] FCM 알림 즉시 발송 시도 - recipientId: {}", recipient.userId());
            outboxDispatchUseCase.sendPendingOutboxImmediately(recipient.userId());
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
