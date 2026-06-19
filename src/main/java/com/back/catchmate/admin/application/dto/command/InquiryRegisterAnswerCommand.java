package com.back.catchmate.admin.application.dto.command;


public record InquiryRegisterAnswerCommand(
        Long inquiryId,
        String content
) {
}
