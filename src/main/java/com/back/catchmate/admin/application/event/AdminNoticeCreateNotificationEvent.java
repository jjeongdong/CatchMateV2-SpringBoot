package com.back.catchmate.admin.application.event;

import com.back.catchmate.notice.domain.model.Notice;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import com.back.catchmate.user.domain.model.User;

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
