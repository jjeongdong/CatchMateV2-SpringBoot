package com.back.catchmate.inquiry.application.port.in;

import com.back.catchmate.inquiry.application.dto.command.InquiryCreateCommand;
import com.back.catchmate.inquiry.application.dto.response.InquiryCreateResponse;

public interface InquiryClientCommandUseCase {
    InquiryCreateResponse createInquiry(Long userId, InquiryCreateCommand command);
}
