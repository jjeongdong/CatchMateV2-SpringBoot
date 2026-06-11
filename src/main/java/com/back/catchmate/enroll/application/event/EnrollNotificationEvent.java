package com.back.catchmate.enroll.application.event;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import com.back.catchmate.user.domain.model.User;

public record EnrollNotificationEvent(
        User recipient,
        User sender,
        Board board,
        String title,
        String body,
        String type,
        Long referenceId,
        boolean pushEnabled
) {
    public static EnrollNotificationEvent of(
            NotificationTemplate template,
            User recipient,
            User sender,
            Board board,
            String type
    ) {
        return of(template, recipient, sender, board, type, board.getId());
    }

    public static EnrollNotificationEvent of(
            NotificationTemplate template,
            User recipient,
            User sender,
            Board board,
            String type,
            Long referenceId
    ) {
        String title = (template == NotificationTemplate.ENROLL_REQUEST || template == NotificationTemplate.ENROLL_CANCEL)
                ? template.formatTitle(sender.getNickName())
                : template.getTitle();
        String body = template.formatBody(board.getTitle());
        boolean pushEnabled = template != NotificationTemplate.ENROLL_CANCEL;

        return new EnrollNotificationEvent(
                recipient, sender, board, title, body, type, referenceId, pushEnabled
        );
    }
}
