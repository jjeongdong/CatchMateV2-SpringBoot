package com.back.catchmate.admin.application.dto.command;


public record InquiryAnswerCommand(
        Long inquiryId,
        String content
) {
}
