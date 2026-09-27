package com.back.catchmate.notification.presentation;

import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.notification.application.dto.result.NotificationReadAllResult;
import com.back.catchmate.notification.application.dto.result.NotificationResult;
import com.back.catchmate.notification.application.dto.result.NotificationUnreadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자] 알림 API")
public interface NotificationApiDocs {

    @Operation(
            summary = "내 알림 목록 조회 (무한 스크롤)",
            description = "최신순으로 조회합니다. cursor 가 없으면 처음부터, 다음 요청에는 응답의 nextCursor 를 넘깁니다.")
    ResponseEntity<CursorPageResult<NotificationResult>> getNotifications(
            @Parameter(hidden = true) Long userId, String cursor, @Min(1) @Max(100) int size);

    @Operation(summary = "알림 상세 조회", description = "알림 상세를 조회합니다. 읽음 처리는 별도 API 로 요청하세요.")
    ResponseEntity<NotificationResult> getNotification(@Parameter(hidden = true) Long userId, Long notificationId);

    @Operation(summary = "알림 읽음 처리", description = "알림 하나를 읽음 상태로 바꿉니다.")
    ResponseEntity<Void> markNotificationAsRead(@Parameter(hidden = true) Long userId, Long notificationId);

    @Operation(summary = "알림 삭제", description = "알림 하나를 삭제합니다.")
    ResponseEntity<Void> deleteNotification(@Parameter(hidden = true) Long userId, Long notificationId);

    @Operation(summary = "읽지 않은 알림 존재 여부", description = "읽지 않은 알림이 하나라도 있는지 확인합니다.")
    ResponseEntity<NotificationUnreadResult> getUnread(@Parameter(hidden = true) Long userId);

    @Operation(summary = "내 알림 전체 읽음 처리", description = "읽지 않은 알림을 모두 읽음 처리하고 바뀐 건수를 돌려줍니다.")
    ResponseEntity<NotificationReadAllResult> readAllNotifications(@Parameter(hidden = true) Long userId);
}
