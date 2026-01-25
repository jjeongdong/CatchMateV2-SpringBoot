package com.back.catchmate.application.inquiry.dto.command;

import inquiry.enums.InquiryType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryCreateCommand {
    private InquiryType type;
    private String title;
    private String content;
}
