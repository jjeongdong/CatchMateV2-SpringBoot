package com.back.catchmate.inquiry.dto.response;

import java.time.LocalDateTime;

public record InquirySummary(
        Long inquiryId,
        Long userId,
        String type,
        String content,
        String answer,
        String status,
        LocalDateTime createdAt) {}
