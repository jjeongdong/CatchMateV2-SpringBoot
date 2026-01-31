package com.back.catchmate.api.admin.dto.request;

import com.back.catchmate.application.admin.dto.command.InquiryAnswerCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryAnswerRequest {
    @NotBlank(message = "답변 내용은 필수입니다.")
    private String content;

    public InquiryAnswerCommand toCommand(Long inquiryId) {
        return InquiryAnswerCommand.builder()
                .inquiryId(inquiryId)
                .content(content)
                .build();
    }
}
