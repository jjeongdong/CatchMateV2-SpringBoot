package com.back.catchmate.inquiry.application.dto.command;

public record InquiryInternalRegisterAnswerCommand(
        Long inquiryId,
        String content
) {
}
