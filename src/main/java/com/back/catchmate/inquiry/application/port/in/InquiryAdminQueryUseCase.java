package com.back.catchmate.inquiry.application.port.in;

import com.back.catchmate.inquiry.application.dto.response.InquiryInternalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InquiryAdminQueryUseCase {
    InquiryInternalResponse getInquiry(Long inquiryId);

    Page<InquiryInternalResponse> getInquiryList(Pageable pageable);

    Page<InquiryInternalResponse> getInquiryListByUser(Long userId, Pageable pageable);

    long getTotalInquiryCount();

    long getWaitingInquiryCount();
}
