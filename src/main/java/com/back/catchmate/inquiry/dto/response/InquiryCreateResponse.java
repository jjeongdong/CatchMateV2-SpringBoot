package com.back.catchmate.inquiry.dto.response;

import java.time.LocalDateTime;

public record InquiryCreateResponse(
        Long inquiryId,
        LocalDateTime createdAt
) {
}
