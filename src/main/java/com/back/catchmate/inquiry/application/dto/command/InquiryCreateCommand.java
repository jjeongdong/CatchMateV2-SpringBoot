package com.back.catchmate.inquiry.application.dto.command;

import com.back.catchmate.inquiry.domain.enums.InquiryType;
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
