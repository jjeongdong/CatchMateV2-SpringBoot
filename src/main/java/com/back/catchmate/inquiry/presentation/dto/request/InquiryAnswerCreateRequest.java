package com.back.catchmate.inquiry.presentation.dto.request;

import com.back.catchmate.inquiry.application.dto.command.InquiryAnswerCreateCommand;
import jakarta.validation.constraints.NotBlank;

public record InquiryAnswerCreateRequest(@NotBlank(message = "답변 내용은 필수입니다.") String content) {
    public InquiryAnswerCreateCommand toCommand() {
        return new InquiryAnswerCreateCommand(content);
    }
}
