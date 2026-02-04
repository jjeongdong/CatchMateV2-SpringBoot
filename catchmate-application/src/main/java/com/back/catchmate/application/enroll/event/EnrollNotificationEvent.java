package com.back.catchmate.application.enroll.event;

import com.back.catchmate.domain.board.model.Board;
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
}
