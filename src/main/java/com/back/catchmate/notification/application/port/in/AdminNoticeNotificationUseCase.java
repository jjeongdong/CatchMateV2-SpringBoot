package com.back.catchmate.notification.application.port.in;

public interface AdminNoticeNotificationUseCase {
    void saveOnNoticeCreated(Long noticeId, String noticeTitle);
}
