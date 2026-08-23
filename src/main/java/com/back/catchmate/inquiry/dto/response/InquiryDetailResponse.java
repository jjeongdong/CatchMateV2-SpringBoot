package com.back.catchmate.inquiry.dto.response;

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
