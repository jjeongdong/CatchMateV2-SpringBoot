package com.back.catchmate.notification.application;

import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.exception.NotificationCursorInvalidException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

// 알림 목록 정렬 키(생성 시각, id)를 응답의 nextCursor 문자열 하나로 주고받는다 (BoardCursor 와 같은 형식).
public record NotificationCursor(LocalDateTime createdAt, Long notificationId) {
    private static final String DELIMITER = "|";

    public static NotificationCursor from(Notification notification) {
        return new NotificationCursor(notification.getCreatedAt(), notification.getId());
    }

    public String encode() {
        String raw = createdAt + DELIMITER + notificationId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static NotificationCursor decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int delimiterIndex = raw.indexOf(DELIMITER);
            if (delimiterIndex < 0) {
                throw new NotificationCursorInvalidException();
            }
            return new NotificationCursor(
                    LocalDateTime.parse(raw.substring(0, delimiterIndex)),
                    Long.valueOf(raw.substring(delimiterIndex + 1)));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            // base64·숫자 형식 오류(NumberFormatException 포함)는 앱이 커서를 변조했거나 손상된 경우다.
            throw new NotificationCursorInvalidException();
        }
    }
}
