package com.back.catchmate.admin.application.dto.response;

import java.time.LocalDateTime;

public record InquiryAnswerResponse(
        Long inquiryId,
        Long userId,
        LocalDateTime answeredAt
) {
    public static InquiryAnswerResponse of(Long inquiryId, Long userId) {
        return new InquiryAnswerResponse(
                inquiryId,
                userId,
                LocalDateTime.now()
        );
    }
}
