package com.back.catchmate.notification.application;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.notification.domain.NotificationTemplate;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신청 알림. save… 는 신청 트랜잭션 안에서 인앱 알림·아웃박스를 쌓고,
 * dispatch… 는 커밋 후 트랜잭션 없이 실시간 알림과 즉시 푸시를 시도한다(FCM 호출 중 커넥션을 잡지 않기 위해).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollNotificationService {
    private final BoardQueryApi boardQueryApi;
    private final UserQueryApi userQueryApi;
    private final NotificationRepository notificationRepository;
    private final OutboxWriter outboxWriter;
    private final OutboxDispatcher outboxDispatcher;
    private final RealtimeNotificationPublisher realtimeNotificationPublisher;

    @Transactional
    public void saveOnEnrollRequested(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        UserInfo applicant = userQueryApi.getInfo(applicantId);
        BoardInfo board = boardQueryApi.getInfo(boardId);
        String title = NotificationTemplate.ENROLL_REQUEST.formatTitle(applicant.nickName());
        String body = NotificationTemplate.ENROLL_REQUEST.formatBody(board.title());
        saveWithOutbox(
                boardOwnerId, applicantId, boardId, enrollId, title, body, NotificationPayload.TYPE_ENROLL_REQUEST);
    }

    @Transactional
    public void saveOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        BoardInfo board = boardQueryApi.getInfo(boardId);
        String title = NotificationTemplate.ENROLL_ACCEPT.title();
        String body = NotificationTemplate.ENROLL_ACCEPT.formatBody(board.title());
        saveWithOutbox(
                applicantId, boardOwnerId, boardId, enrollId, title, body, NotificationPayload.TYPE_ENROLL_ACCEPTED);
    }

    @Transactional
    public void saveOnEnrollRejected(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        BoardInfo board = boardQueryApi.getInfo(boardId);
        String title = NotificationTemplate.ENROLL_REJECT.title();
        String body = NotificationTemplate.ENROLL_REJECT.formatBody(board.title());
        saveWithOutbox(
                applicantId, boardOwnerId, boardId, enrollId, title, body, NotificationPayload.TYPE_ENROLL_REJECTED);
    }

    // 취소는 인앱 알림만 남기고 푸시하지 않는다 (옛 동작).
    @Transactional
    public void saveOnEnrollCancelled(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        UserInfo applicant = userQueryApi.getInfo(applicantId);
        String title = NotificationTemplate.ENROLL_CANCEL.formatTitle(applicant.nickName());
        notificationRepository.save(
                Notification.create(boardOwnerId, applicantId, boardId, title, AlarmType.ENROLL, enrollId));
    }

    public void dispatchOnEnrollRequested(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        UserInfo recipient = userQueryApi.getInfo(boardOwnerId);
        if (!recipient.enrollAlarmEnabled()) {
            log.debug("신청 알림 꺼짐, 발송 생략 enrollId={}, recipientId={}", enrollId, boardOwnerId);
            return;
        }
        UserInfo applicant = userQueryApi.getInfo(applicantId);
        BoardInfo board = boardQueryApi.getInfo(boardId);
        String title = NotificationTemplate.ENROLL_REQUEST.formatTitle(applicant.nickName());
        String body = NotificationTemplate.ENROLL_REQUEST.formatBody(board.title());
        dispatch(recipient.userId(), boardId, title, body, NotificationPayload.TYPE_ENROLL_REQUEST, true);
    }

    public void dispatchOnEnrollAccepted(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        UserInfo recipient = userQueryApi.getInfo(applicantId);
        if (!recipient.enrollAlarmEnabled()) {
            log.debug("신청 알림 꺼짐, 발송 생략 enrollId={}, recipientId={}", enrollId, applicantId);
            return;
        }
        BoardInfo board = boardQueryApi.getInfo(boardId);
        String title = NotificationTemplate.ENROLL_ACCEPT.title();
        String body = NotificationTemplate.ENROLL_ACCEPT.formatBody(board.title());
        dispatch(recipient.userId(), boardId, title, body, NotificationPayload.TYPE_ENROLL_ACCEPTED, true);
    }

    public void dispatchOnEnrollRejected(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        UserInfo recipient = userQueryApi.getInfo(applicantId);
        if (!recipient.enrollAlarmEnabled()) {
            log.debug("신청 알림 꺼짐, 발송 생략 enrollId={}, recipientId={}", enrollId, applicantId);
            return;
        }
        BoardInfo board = boardQueryApi.getInfo(boardId);
        String title = NotificationTemplate.ENROLL_REJECT.title();
        String body = NotificationTemplate.ENROLL_REJECT.formatBody(board.title());
        dispatch(recipient.userId(), boardId, title, body, NotificationPayload.TYPE_ENROLL_REJECTED, true);
    }

    public void dispatchOnEnrollCancelled(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {
        UserInfo recipient = userQueryApi.getInfo(boardOwnerId);
        if (!recipient.enrollAlarmEnabled()) {
            log.debug("신청 알림 꺼짐, 발송 생략 enrollId={}, recipientId={}", enrollId, boardOwnerId);
            return;
        }
        UserInfo applicant = userQueryApi.getInfo(applicantId);
        BoardInfo board = boardQueryApi.getInfo(boardId);
        String title = NotificationTemplate.ENROLL_CANCEL.formatTitle(applicant.nickName());
        String body = NotificationTemplate.ENROLL_CANCEL.formatBody(board.title());
        dispatch(recipient.userId(), boardId, title, body, NotificationPayload.TYPE_ENROLL_CANCEL, false);
    }

    private void saveWithOutbox(
            Long recipientId, Long senderId, Long boardId, Long enrollId, String title, String body, String type) {
        notificationRepository.save(
                Notification.create(recipientId, senderId, boardId, title, AlarmType.ENROLL, enrollId));

        UserInfo recipient = userQueryApi.getInfo(recipientId);
        if (!recipient.enrollAlarmEnabled() || recipient.fcmToken() == null) {
            log.debug("신청 알림 꺼짐 또는 토큰 없음, 아웃박스 생략 enrollId={}, recipientId={}", enrollId, recipientId);
            return;
        }
        outboxWriter.write(
                recipient.userId(),
                recipient.fcmToken(),
                title,
                body,
                NotificationPayload.enroll(type, boardId, title, body));
    }

    private void dispatch(Long recipientId, Long boardId, String title, String body, String type, boolean push) {
        realtimeNotificationPublisher.publish(recipientId, NotificationPayload.enroll(type, boardId, title, body));
        if (push) {
            outboxDispatcher.sendPendingOutboxImmediately(recipientId);
        }
    }
}
