package com.back.catchmate.notification.adapter.in.web.controller;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.notification.application.dto.response.ReadAllNotificationsResponse;
import com.back.catchmate.notification.application.port.in.NotificationClientCommandUseCase;
import com.back.catchmate.notification.application.port.in.NotificationClientQueryUseCase;
import com.back.catchmate.notification.application.dto.response.NotificationResponse;
import com.back.catchmate.notification.application.dto.response.UnreadNotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 알림 관련 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationClientCommandUseCase notificationClientCommandUseCase;
    private final NotificationClientQueryUseCase notificationClientQueryUseCase;

    @Operation(summary = "알림 상세 조회", description = "알림 상세 데이터를 조회합니다. 읽음 처리는 별도 API 로 요청하세요.")
    @GetMapping("/{notificationId}")
    public NotificationResponse getNotification(@Parameter(hidden = true) @AuthUser Long userId,
                                                @PathVariable Long notificationId
    ) {
        return notificationClientQueryUseCase.getNotification(userId, notificationId);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/{notificationId}/read")
    public void markNotificationAsRead(@Parameter(hidden = true) @AuthUser Long userId,
                                       @PathVariable Long notificationId
    ) {
        notificationClientCommandUseCase.markNotificationAsRead(userId, notificationId);
    }

    @Operation(summary = "내 알림 목록 조회", description = "로그인한 사용자의 알림 목록을 페이징하여 조회합니다.")
    @GetMapping
    public PagedResponse<NotificationResponse> getNotificationList(@Parameter(hidden = true) @AuthUser Long userId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size
    ) {
        return notificationClientQueryUseCase.getNotificationList(userId, page, size);
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @DeleteMapping("/{notificationId}")
    public void deleteNotification(@Parameter(hidden = true) @AuthUser Long userId,
                                   @PathVariable Long notificationId
    ) {
        notificationClientCommandUseCase.deleteNotification(userId, notificationId);
    }

    @Operation(summary = "읽지 않은 알림 존재 여부 확인", description = "로그인한 사용자의 읽지 않은 알림이 있는지 확인합니다.")
    @GetMapping("/unread")
    public UnreadNotificationResponse hasUnreadNotifications(@Parameter(hidden = true) @AuthUser Long userId) {
        return notificationClientQueryUseCase.hasUnreadNotifications(userId);
    }

    @Operation(summary = "내 알림 전체 읽음 처리", description = "로그인한 사용자의 읽지 않은 알림을 모두 읽음 처리합니다.")
    @PostMapping("/read-all")
    public ReadAllNotificationsResponse readAllNotifications(@Parameter(hidden = true) @AuthUser Long userId) {
        int updated = notificationClientCommandUseCase.readAllNotifications(userId);
        return new ReadAllNotificationsResponse(updated);
    }
}
