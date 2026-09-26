package com.back.catchmate.inquiry.application.dto.result;

import java.time.LocalDateTime;

public record InquiryAnswerCreateResult(Long inquiryId, Long userId, LocalDateTime answeredAt) {}
