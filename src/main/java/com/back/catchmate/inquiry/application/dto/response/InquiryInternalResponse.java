package com.back.catchmate.inquiry.application.dto.response;

import java.time.LocalDateTime;

public record InquiryInternalResponse(
        Long inquiryId,
        Long userId,
        String type,
        String content,
        String answer,
        String status,
        LocalDateTime createdAt
) {
}
