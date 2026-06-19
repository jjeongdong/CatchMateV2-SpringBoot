package com.back.catchmate.admin.application.dto.response;

import java.time.LocalDateTime;

public record AdminInquiryAnswerResponse(
        Long inquiryId,
        Long userId,
        LocalDateTime answeredAt
) {
    public static AdminInquiryAnswerResponse of(Long inquiryId, Long userId) {
        return new AdminInquiryAnswerResponse(
                inquiryId,
                userId,
                LocalDateTime.now()
        );
    }
}
