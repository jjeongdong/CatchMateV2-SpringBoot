package com.back.catchmate.api.inquiry.dto.request;

import com.back.catchmate.orchestration.inquiry.dto.command.InquiryCreateCommand;
import inquiry.enums.InquiryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryCreateRequest {
    @NotNull(message = "문의 유형을 선택해주세요.")
    private InquiryType type;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    public InquiryCreateCommand toCommand() {
        return InquiryCreateCommand.builder()
                .type(this.type)
                .content(this.content)
                .build();
    }
}
