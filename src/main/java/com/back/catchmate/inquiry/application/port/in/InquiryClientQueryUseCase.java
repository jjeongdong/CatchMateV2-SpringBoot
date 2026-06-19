package com.back.catchmate.inquiry.application.port.in;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.inquiry.application.dto.response.InquiryDetailResponse;

public interface InquiryClientQueryUseCase {
    InquiryDetailResponse getInquiryDetail(Long userId, Long inquiryId);

    PagedResponse<InquiryDetailResponse> getInquiryListByUser(Long userId, int page, int size);
}
