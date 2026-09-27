package com.back.catchmate.notification.presentation;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.notification.application.NotificationCommandService;
import com.back.catchmate.notification.application.NotificationQueryService;
import com.back.catchmate.notification.application.dto.result.NotificationReadAllResult;
import com.back.catchmate.notification.application.dto.result.NotificationResult;
import com.back.catchmate.notification.application.dto.result.NotificationUnreadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationApiDocs {
    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;

    @Override
    @GetMapping
    public ResponseEntity<CursorPageResult<NotificationResult>> getNotifications(
            @AuthUser Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationQueryService.getNotifications(userId, cursor, size));
    }

    @Override
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResult> getNotification(
            @AuthUser Long userId, @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationQueryService.getNotification(userId, notificationId));
    }

    @Override
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(@AuthUser Long userId, @PathVariable Long notificationId) {
        notificationCommandService.markNotificationAsRead(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@AuthUser Long userId, @PathVariable Long notificationId) {
        notificationCommandService.deleteNotification(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/unread")
    public ResponseEntity<NotificationUnreadResult> getUnread(@AuthUser Long userId) {
        return ResponseEntity.ok(notificationQueryService.getUnread(userId));
    }

    @Override
    @PostMapping("/read-all")
    public ResponseEntity<NotificationReadAllResult> readAllNotifications(@AuthUser Long userId) {
        return ResponseEntity.ok(notificationCommandService.readAllNotifications(userId));
    }
}
