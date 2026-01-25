package com.back.catchmate.api.inquiry.dto.request;

import com.back.catchmate.application.inquiry.dto.command.InquiryCreateCommand;
import inquiry.enums.InquiryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryCreateRequest {
    @NotNull(message = "문의 유형을 선택해주세요.")
    private InquiryType type;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    public InquiryCreateCommand toCommand() {
        return InquiryCreateCommand.builder()
                .type(this.type)
                .title(this.title)
                .content(this.content)
                .build();
    }
}
