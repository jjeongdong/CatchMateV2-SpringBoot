package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.inquiry.domain.model.Inquiry;

public interface InquiryFetchPort {
    Inquiry getInquiryEntity(Long inquiryId);
    DomainPage<Inquiry> getInquiryList(DomainPageable pageable);
    long getTotalInquiryCount();
    long getWaitingInquiryCount();
    Inquiry updateInquiry(Inquiry inquiry);
}
