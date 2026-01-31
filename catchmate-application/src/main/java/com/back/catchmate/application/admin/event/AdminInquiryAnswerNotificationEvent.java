package com.back.catchmate.application.admin.event;

import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.user.model.User;

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
}

