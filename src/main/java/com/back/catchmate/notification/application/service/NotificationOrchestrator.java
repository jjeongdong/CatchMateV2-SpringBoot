package com.back.catchmate.notification.application.service;

import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.notification.application.service.NotificationRetryService;
import com.back.catchmate.notification.application.service.NotificationService;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.notification.application.dto.response.NotificationResponse;
import com.back.catchmate.notification.application.dto.response.UnreadNotificationResponse;
import com.back.catchmate.user.domain.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationOrchestrator {
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;
    private final EnrollService enrollService;

    @Transactional
    public NotificationResponse getNotification(Long userId, Long notificationId) {
        // 1. 서비스 호출 (조회 및 읽음 처리 로직 위임)
        Notification notification = notificationService.getNotification(notificationId);

        AcceptStatus acceptStatus = null;
        if (notification.getType() == AlarmType.ENROLL && notification.getTargetId() != null) {
            acceptStatus = enrollService.findAcceptStatusById(notification.getTargetId())
                    .orElse(null);
        }

        // 2. DTO 변환 및 반환
        return NotificationResponse.from(notification, acceptStatus);
    }

    public PagedResponse<NotificationResponse> getNotificationList(Long userId, int page, int size) {
        DomainPageable domainPageable = DomainPageable.of(page, size);

        // 1. 서비스 호출
        DomainPage<Notification> notificationPage = notificationService.getNotificationList(userId, domainPageable);

        // 2. 신청 상태 정보 일괄 조회 (N+1 방지)
        List<Long> enrollIds = notificationPage.getContent().stream()
                .filter(n -> n.getType() == AlarmType.ENROLL && n.getTargetId() != null)
                .map(Notification::getTargetId)
                .toList();

        Map<Long, AcceptStatus> enrollStatusMap = enrollService.getAcceptStatusMapByIds(enrollIds);

        // 3. DTO 변환
        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(notification -> {
                    AcceptStatus status = (notification.getType() == AlarmType.ENROLL)
                            ? enrollStatusMap.get(notification.getTargetId())
                            : null;
                    return NotificationResponse.from(notification, status);
                })
                .toList();

        return new PagedResponse<>(notificationPage, responses);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        notificationService.deleteNotification(userId, notificationId);
    }

    public UnreadNotificationResponse hasUnreadNotifications(Long userId) {
        boolean hasUnread = notificationService.hasUnreadNotifications(userId);
        return UnreadNotificationResponse.of(hasUnread);
    }

    @Transactional
    public int readAllNotifications(Long userId) {
        return notificationService.markAllRead(userId);
    }

    public void processPendingNotifications() {
        notificationRetryService.processPendingNotifications();
    }
}
