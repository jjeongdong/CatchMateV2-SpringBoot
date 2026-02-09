package com.back.catchmate.orchestration.inquiry.dto.command;

import com.back.catchmate.inquiry.enums.InquiryType;
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
