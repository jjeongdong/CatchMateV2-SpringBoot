package com.back.catchmate.application.enroll.event;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.notification.model.NotificationTemplate;
import com.back.catchmate.domain.user.model.User;

public record EnrollNotificationEvent(
        User recipient,
        User sender,
        Board board,
        String title,
        String body,
        String type,
        Long referenceId
) {
    public static EnrollNotificationEvent of(
            NotificationTemplate template,
            User recipient,
            User sender,
            Board board,
            String type
    ) {
        String title = template.getTitle();
        String body = template.formatBody(board.getTitle());

        return new EnrollNotificationEvent(
                recipient, sender, board, title, body, type, board.getId()
        );
    }
}
