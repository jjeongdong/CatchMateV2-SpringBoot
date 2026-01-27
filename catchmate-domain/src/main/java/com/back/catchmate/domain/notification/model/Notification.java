package com.back.catchmate.domain.notification.model;

import com.back.catchmate.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;
import user.enums.AlarmType;

import java.time.LocalDateTime;

@Getter
@Builder
public class Notification {
    private Long id;
    private User user;
    private String title;
    private String body;
    private AlarmType type;
    private Long targetId;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static Notification createNotification(User user, String title, String body, AlarmType type, Long targetId) {
        return Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .targetId(targetId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
