package com.back.catchmate.orchestration.inquiry.dto.command;

import inquiry.enums.InquiryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InquiryCreateCommand {
    private InquiryType type;
    private String content;
}
