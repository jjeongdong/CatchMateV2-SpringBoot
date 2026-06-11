package com.back.catchmate.admin.application.event;

import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import com.back.catchmate.user.domain.model.User;

/**
 * 관리자 문의 답변 등록 후, 사용자에게 푸시 알림을 전송하기 위한 이벤트.
 */
public record AdminInquiryAnswerNotificationEvent(
        User recipient,
        Inquiry inquiry,
        String title,
        String body,
        String type
) {
    public static AdminInquiryAnswerNotificationEvent of(User recipient, Inquiry inquiry) {
        String title = NotificationTemplate.INQUIRY_ANSWER.getTitle();
        String body = NotificationTemplate.INQUIRY_ANSWER.getBodyTemplate();

        return new AdminInquiryAnswerNotificationEvent(recipient, inquiry, title, body, "INQUIRY");
    }
}

