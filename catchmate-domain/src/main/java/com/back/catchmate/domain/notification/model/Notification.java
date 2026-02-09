package com.back.catchmate.domain.notification.model;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.common.permission.ResourceOwnership;
import com.back.catchmate.domain.user.model.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.back.catchmate.user.enums.AlarmType;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification implements ResourceOwnership {
    private Long id;
    private User user;
    private User sender;
    private Board board;
    private String title;
    private AlarmType type;
    private Long targetId;
    private boolean read;
    private LocalDateTime createdAt;

    public static Notification createNotification(User user, User sender, Board board, String title, AlarmType type, Long targetId) {
        return Notification.builder()
                .user(user)
                .sender(sender)
                .board(board)
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
        return user.getId();
    }
}
