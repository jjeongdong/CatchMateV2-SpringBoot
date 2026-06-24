package com.back.catchmate.notification.application.port.in;

public interface AdminInquiryNotificationDispatchUseCase {
    void dispatchOnInquiryAnswered(Long inquiryId, Long inquiryAuthorId);
}
