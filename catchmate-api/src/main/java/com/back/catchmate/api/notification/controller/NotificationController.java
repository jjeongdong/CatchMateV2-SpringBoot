package com.back.catchmate.api.notification.controller;

import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.application.notification.NotificationUseCase;
import com.back.catchmate.application.notification.dto.response.NotificationResponse;
import com.back.catchmate.global.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 알림 관련 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationUseCase notificationUseCase;

    @Operation(summary = "내 알림 목록 조회", description = "로그인한 사용자의 알림 목록을 페이징하여 조회합니다.")
    @GetMapping
    public PagedResponse<NotificationResponse> getMyNotifications(
            @Parameter(hidden = true) @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return notificationUseCase.getNotifications(userId, page, size);
    }

    @Operation(summary = "알림 상세 조회", description = "알림을 상세 조회하고 읽음 상태로 변경합니다.")
    @GetMapping("/{notificationId}")
    public NotificationResponse getNotification(
            @Parameter(hidden = true) @AuthUser Long userId,
            @PathVariable Long notificationId
    ) {
        return notificationUseCase.getNotification(userId, notificationId);
    }


}
