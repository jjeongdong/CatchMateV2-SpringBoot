package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.EnrollNotificationUseCase;
import com.back.catchmate.notification.application.port.in.NotificationInternalCommandUseCase;
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

/**
 * Enroll 알림의 영속화(Notification + Outbox 저장) 전용 서비스.
 * 실시간/FCM 발송은 {@link EnrollNotificationDispatchService}(비트랜잭션) 가 담당한다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EnrollNotificationService implements EnrollNotificationUseCase {
    private static final String TYPE_ENROLL_REQUEST = "ENROLL_REQUEST";
    private static final String TYPE_ENROLL_ACCEPTED = "ENROLL_ACCEPTED";
    private static final String TYPE_ENROLL_REJECTED = "ENROLL_REJECTED";
    private static final String TYPE_ENROLL_CANCEL = "ENROLL_CANCEL";

    private final BoardFetchPort boardFetchPort;
    private final UserFetchPort userFetchPort;
    private final OutboxSaveUseCase outboxSaveUseCase;
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
    public void saveOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        log.info("[Enroll알림] saveOnEnrollAccepted 호출 - enrollId: {}, boardId: {}, applicantId: {}, boardOwnerId: {}",
                enrollId, boardId, applicantId, boardOwnerId);
        NotificationBoardInfo board = boardFetchPort.getBoard(boardId);
        String title = NotificationTemplate.ENROLL_ACCEPT.getTitle();
        String body = NotificationTemplate.ENROLL_ACCEPT.formatBody(board.title());

        saveNotificationAndOutbox(applicantId, boardOwnerId, boardId, enrollId, title, body, TYPE_ENROLL_ACCEPTED, true);
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

    private Map<String, String> createNotificationData(String type, Long boardId, String title, String body) {
        return Map.of(
                "type", type,
                "boardId", String.valueOf(boardId),
                "title", title,
                "body", body
        );
    }
}
