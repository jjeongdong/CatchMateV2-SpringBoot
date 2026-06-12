package com.back.catchmate.admin.adapter.in.web.dto.request;

import com.back.catchmate.admin.application.dto.command.InquiryAnswerCommand;
import jakarta.validation.constraints.NotBlank;

public record InquiryAnswerRequest(
        @NotBlank(message = "답변 내용은 필수입니다.") String content
) {
    public InquiryAnswerCommand toCommand(Long inquiryId) {
        return new InquiryAnswerCommand(
                inquiryId,
                content
        );
    }
}
