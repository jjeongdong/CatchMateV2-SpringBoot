package com.back.catchmate.authorization.finder;

import com.back.catchmate.application.inquiry.service.InquiryService;
import com.back.catchmate.domain.common.permission.DomainFinder;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryPermissionFinder implements DomainFinder<Inquiry> {
    private final InquiryService inquiryService;

    @Override
    public Inquiry searchById(Long inquiryId) {
        return inquiryService.getInquiry(inquiryId);
    }
}
