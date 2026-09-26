package com.back.catchmate.inquiry.application.dto.result;

import com.back.catchmate.inquiry.domain.Inquiry;
import java.time.LocalDateTime;

public record InquiryCreateResult(Long inquiryId, LocalDateTime createdAt) {
    public static InquiryCreateResult from(Inquiry inquiry) {
        return new InquiryCreateResult(inquiry.getId(), inquiry.getCreatedAt());
    }
}
