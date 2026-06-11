package com.back.catchmate.global.authorization.finder;

import com.back.catchmate.inquiry.application.service.InquiryService;
import com.back.catchmate.global.authorization.common.DomainFinder;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryPermissionFinder implements DomainFinder<Inquiry> {
    private final InquiryService inquiryService;

    @Override
    public Inquiry searchById(Long inquiryId) {
        return inquiryService.getInquiryEntity(inquiryId);
    }
}
