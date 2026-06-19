package com.back.catchmate.inquiry.application.dto.response;

import java.time.LocalDateTime;

public record InquiryCreateResponse(
        Long inquiryId,
        LocalDateTime createdAt
) {
}
