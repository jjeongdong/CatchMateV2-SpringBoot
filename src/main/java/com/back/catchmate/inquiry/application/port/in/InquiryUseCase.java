package com.back.catchmate.inquiry.application.port.in;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.inquiry.application.dto.command.InquiryCreateCommand;
import com.back.catchmate.inquiry.application.dto.response.InquiryCreateResponse;
import com.back.catchmate.inquiry.application.dto.response.InquiryDetailResponse;

public interface InquiryUseCase {
    InquiryCreateResponse createInquiry(Long userId, InquiryCreateCommand command);
    InquiryDetailResponse getInquiry(Long inquiryId);
    PagedResponse<InquiryDetailResponse> getInquiryListByUser(Long userId, int page, int size);
}
