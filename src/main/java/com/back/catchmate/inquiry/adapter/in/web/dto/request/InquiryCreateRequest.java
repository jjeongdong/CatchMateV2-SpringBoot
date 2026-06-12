package com.back.catchmate.inquiry.adapter.in.web.dto.request;

import com.back.catchmate.inquiry.application.dto.command.InquiryCreateCommand;
import com.back.catchmate.inquiry.domain.enums.InquiryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record InquiryCreateRequest(
        @NotNull(message = "문의 유형을 선택해주세요.") InquiryType type,
        @NotBlank(message = "내용을 입력해주세요.") String content
) {
    public InquiryCreateCommand toCommand() {
        return new InquiryCreateCommand(
                this.type,
                this.content
        );
    }
}
