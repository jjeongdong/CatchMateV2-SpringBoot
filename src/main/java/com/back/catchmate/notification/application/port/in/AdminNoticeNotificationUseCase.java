package com.back.catchmate.notification.application.port.in;

public interface AdminNoticeNotificationUseCase {
    void saveOnNoticeCreated(Long noticeId, String noticeTitle);

    void dispatchOnNoticeCreated(Long noticeId, String noticeTitle);
}
