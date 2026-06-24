package com.back.catchmate.notification.application.port.in;

public interface AdminInquiryNotificationUseCase {
    void saveOnInquiryAnswered(Long inquiryId, Long inquiryAuthorId);
}
