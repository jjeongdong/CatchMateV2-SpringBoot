package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.InquiryFetchPort;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.inquiry.application.service.InquiryService;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInquiryFetchAdapter implements InquiryFetchPort {
    private final InquiryService inquiryService;

    @Override
    public Inquiry getInquiryEntity(Long inquiryId) {
        return inquiryService.getInquiryEntity(inquiryId);
    }

    @Override
    public DomainPage<Inquiry> getInquiryList(DomainPageable pageable) {
        return inquiryService.getInquiryList(pageable);
    }

    @Override
    public long getTotalInquiryCount() {
        return inquiryService.getTotalInquiryCount();
    }

    @Override
    public long getWaitingInquiryCount() {
        return inquiryService.getWaitingInquiryCount();
    }

    @Override
    public Inquiry updateInquiry(Inquiry inquiry) {
        return inquiryService.updateInquiry(inquiry);
    }
}
