package com.back.catchmate.inquiry.application.dto.response;

import java.time.LocalDateTime;

public record InquiryDetailResponse(
        Long inquiryId,
        String nickname,
        String type,
        String content,
        String answer,
        String status,
        LocalDateTime createdAt
) {
}
