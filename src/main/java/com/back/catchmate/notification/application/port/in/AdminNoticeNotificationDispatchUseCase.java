package com.back.catchmate.notification.application.port.in;

public interface AdminNoticeNotificationDispatchUseCase {
    void dispatchOnNoticeCreated(Long noticeId, String noticeTitle);
}
