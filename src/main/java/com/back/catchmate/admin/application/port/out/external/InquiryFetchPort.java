package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminInquiryInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InquiryFetchPort {
    AdminInquiryInfo getInquiry(Long inquiryId);

    Page<AdminInquiryInfo> getInquiryList(Pageable pageable);

    long getTotalInquiryCount();

    long getWaitingInquiryCount();
}
