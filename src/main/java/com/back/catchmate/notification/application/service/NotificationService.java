package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.out.EnrollFetchPort;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.notification.application.dto.response.NotificationResponse;
import com.back.catchmate.notification.application.dto.response.UnreadNotificationResponse;
import com.back.catchmate.notification.application.port.in.NotificationUseCase;
import com.back.catchmate.notification.application.port.out.NotificationRepository;
import com.back.catchmate.notification.application.service.NotificationRetryService;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.user.domain.enums.AlarmType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificationRepository notificationRepository;

    private final NotificationRetryService notificationRetryService;

    private final EnrollFetchPort enrollFetchPort;

    @Transactional
    public NotificationResponse getNotification(Long userId, Long notificationId) {
        // 1. 서비스 호출 (조회 및 읽음 처리 로직 위임)
        Notification notification = getNotification(notificationId);

        AcceptStatus acceptStatus = null;
        if (notification.getType() == AlarmType.ENROLL && notification.getTargetId() != null) {
            acceptStatus = enrollFetchPort.findAcceptStatusById(notification.getTargetId())
                    .orElse(null);
        }

        // 2. DTO 변환 및 반환
        return NotificationResponse.from(notification, acceptStatus);
    }

    public PagedResponse<NotificationResponse> getNotificationList(Long userId, int page, int size) {
        DomainPageable domainPageable = DomainPageable.of(page, size);

        // 1. 서비스 호출
        DomainPage<Notification> notificationPage = getNotificationList(userId, domainPageable);

        // 2. 신청 상태 정보 일괄 조회 (N+1 방지)
        List<Long> enrollIds = notificationPage.getContent().stream()
                .filter(n -> n.getType() == AlarmType.ENROLL && n.getTargetId() != null)
                .map(Notification::getTargetId)
                .toList();

        Map<Long, AcceptStatus> enrollStatusMap = enrollFetchPort.getAcceptStatusMapByIds(enrollIds);

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
        deleteNotificationEntity(userId, notificationId);
    }

    public UnreadNotificationResponse hasUnreadNotifications(Long userId) {
        boolean hasUnread = existsUnreadNotifications(userId);
        return UnreadNotificationResponse.of(hasUnread);
    }

    @Transactional
    public int readAllNotifications(Long userId) {
        return markAllRead(userId);
    }

    public void processPendingNotifications() {
        notificationRetryService.processPendingNotifications();
    }

    public void createNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    // 조회와 읽음 처리를 함께 수행하는 비즈니스 메서드
    public Notification getNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
        
        return notification;
    }

    public DomainPage<Notification> getNotificationList(Long userId, DomainPageable pageable) {
        return notificationRepository.findAllByUserId(userId, pageable);
    }

    public void deleteNotificationEntity(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 소유권 검증 로직이 필요하다면 여기에 추가 (현재는 AOP에서 처리 중)
        notificationRepository.delete(notification);
    }

    public boolean existsUnreadNotifications(Long userId) {
        return notificationRepository.hasUnreadNotifications(userId);
    }

    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }
}
