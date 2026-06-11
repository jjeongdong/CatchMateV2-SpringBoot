package com.back.catchmate.admin.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.inquiry.domain.model.Inquiry;

public interface InquiryFetchPort {
    Inquiry getInquiryEntity(Long inquiryId);
    Page<Inquiry> getInquiryList(Pageable pageable);
    long getTotalInquiryCount();
    long getWaitingInquiryCount();
    Inquiry updateInquiry(Inquiry inquiry);
}
