package com.back.catchmate.inquiry.application.dto.response;

import java.time.LocalDateTime;

public record InquiryCreateResponse(
        Long inquiryId,
        LocalDateTime createdAt
) {
    public static InquiryCreateResponse of(Long inquiryId) {
        return new InquiryCreateResponse(
                inquiryId,
                LocalDateTime.now()
        );
    }
}
