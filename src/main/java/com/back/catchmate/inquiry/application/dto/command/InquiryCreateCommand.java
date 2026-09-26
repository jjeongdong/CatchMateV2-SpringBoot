package com.back.catchmate.inquiry.application.dto.command;

import com.back.catchmate.inquiry.domain.InquiryType;

public record InquiryCreateCommand(InquiryType type, String content) {}
