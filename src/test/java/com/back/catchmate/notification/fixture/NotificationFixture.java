package com.back.catchmate.notification.fixture;

import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class NotificationFixture {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 12, 0);

    private NotificationFixture() {}

    // 저장 없이 쓰는 단위 테스트용이라 id·시각을 리플렉션으로 채운다.
    public static Notification notification(
            Long notificationId, Long userId, Long senderId, Long boardId, AlarmType type, Long targetId) {
        Notification notification = Notification.create(userId, senderId, boardId, "알림", type, targetId);
        ReflectionTestUtils.setField(notification, "id", notificationId);
        ReflectionTestUtils.setField(notification, "createdAt", NOW);
        return notification;
    }

    public static NotificationOutbox outbox(Long outboxId, Long recipientId, String payload) {
        NotificationOutbox outbox = NotificationOutbox.create(recipientId, "token-" + recipientId, "제목", "본문", payload);
        ReflectionTestUtils.setField(outbox, "id", outboxId);
        ReflectionTestUtils.setField(outbox, "createdAt", NOW);
        return outbox;
    }

    public static UserInfo user(
            Long userId,
            String nickName,
            String fcmToken,
            boolean chatAlarmEnabled,
            boolean enrollAlarmEnabled,
            boolean eventAlarmEnabled) {
        return new UserInfo(
                userId,
                "u@catchmate.com",
                null,
                null,
                'M',
                nickName,
                LocalDate.of(2000, 1, 1),
                "응원형",
                "img-" + userId,
                "ROLE_USER",
                fcmToken,
                null,
                chatAlarmEnabled,
                enrollAlarmEnabled,
                eventAlarmEnabled,
                false,
                NOW,
                NOW);
    }

    public static BoardInfo board(Long boardId, String title, Long gameId) {
        return new BoardInfo(boardId, title, "내용", 4, 1, 9L, 1L, gameId, "N", List.of(), true, NOW, NOW);
    }
}
