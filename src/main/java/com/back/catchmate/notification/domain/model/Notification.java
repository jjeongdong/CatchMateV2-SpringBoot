package com.back.catchmate.notification.domain.model;

import com.back.catchmate.global.authorization.common.ResourceOwnership;
import com.back.catchmate.user.domain.enums.AlarmType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification implements ResourceOwnership {
    private Long id;
    private Long userId;
    private Long senderId;
    private Long boardId;
    private String title;
    private AlarmType type;
    private Long targetId;
    private boolean read;
    private LocalDateTime createdAt;

    public static Notification createNotification(Long userId, Long senderId, Long boardId, String title, AlarmType type, Long targetId) {
        return Notification.builder()
                .userId(userId)
                .senderId(senderId)
                .boardId(boardId)
                .title(title)
                .type(type)
                .targetId(targetId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markAsRead() {
        this.read = true;
    }

    @Override
    public Long getOwnershipId() {
        return userId;
    }
}
