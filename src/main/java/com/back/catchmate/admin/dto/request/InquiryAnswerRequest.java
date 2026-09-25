package com.back.catchmate.admin.dto.request;

import com.back.catchmate.admin.dto.command.InquiryRegisterAnswerCommand;
import jakarta.validation.constraints.NotBlank;

public record InquiryAnswerRequest(@NotBlank(message = "답변 내용은 필수입니다.") String content) {
    public InquiryRegisterAnswerCommand toCommand(Long inquiryId) {
        return new InquiryRegisterAnswerCommand(inquiryId, content);
    }
}
