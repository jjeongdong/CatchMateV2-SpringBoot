package com.back.catchmate.application.admin.event;

import com.back.catchmate.domain.notice.model.Notice;
import com.back.catchmate.domain.notification.model.NotificationTemplate;
import com.back.catchmate.domain.user.model.User;

import java.util.List;

public record AdminNoticeCreateNotificationEvent(
        Notice notice,
        List<User> recipients,
        String title,
        String body,
        String type
) {
    public static AdminNoticeCreateNotificationEvent of(Notice notice, List<User> recipients) {
        String title = NotificationTemplate.NOTICE_CREATED.getTitle();
        String body = NotificationTemplate.NOTICE_CREATED.formatBody(notice.getTitle());

        return new AdminNoticeCreateNotificationEvent(notice, recipients, title, body, "NOTICE");
    }
}
