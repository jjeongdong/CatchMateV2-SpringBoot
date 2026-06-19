package com.back.catchmate.admin.application.port.out.dto;

import java.time.LocalDateTime;

public record AdminInquiryInfo(
        Long inquiryId,
        Long userId,
        String type,
        String content,
        String answer,
        String status,
        LocalDateTime createdAt
) {
}
