package com.back.catchmate.application.notification;

import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.application.notification.dto.response.NotificationResponse;
import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.repository.NotificationRepository;
import com.back.catchmate.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationUseCase {
    private final NotificationService notificationService;

    public PagedResponse<NotificationResponse> getNotifications(Long userId, int page, int size) {
        // 1. 도메인 페이징 객체 생성
        DomainPageable domainPageable = DomainPageable.of(page, size);

        // 2. 리포지토리 조회 (DomainPage 반환)
        DomainPage<Notification> notificationPage = notificationService.getAllNotifications(userId, domainPageable);

        // 3. 도메인 -> 응답 DTO 변환
        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        // 4. PagedResponse로 포장하여 반환
        return new PagedResponse<>(notificationPage, responses);
    }

    @Transactional
    public NotificationResponse getNotification(Long userId, Long notificationId) {
        // 1. 알림 조회
        Notification notification = notificationService.getNotification(userId, notificationId);

        // 2. 읽음 처리
        if (!notification.isRead()) {
            notification.markAsRead();
            notificationService.updateNotification(notification); // 변경사항 저장
        }

        // 3. 응답 반환
        return NotificationResponse.from(notification);
    }
}
