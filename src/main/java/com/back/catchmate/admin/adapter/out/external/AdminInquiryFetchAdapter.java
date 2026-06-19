package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminInquiryInfo;
import com.back.catchmate.admin.application.port.out.external.InquiryFetchPort;
import com.back.catchmate.inquiry.application.dto.response.InquiryInternalResponse;
import com.back.catchmate.inquiry.application.port.in.InquiryAdminQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInquiryFetchAdapter implements InquiryFetchPort {
    private final InquiryAdminQueryUseCase inquiryAdminQueryUseCase;

    @Override
    public AdminInquiryInfo getInquiry(Long inquiryId) {
        return fromInternalResponse(inquiryAdminQueryUseCase.getInquiry(inquiryId));
    }

    @Override
    public Page<AdminInquiryInfo> getInquiryList(Pageable pageable) {
        return inquiryAdminQueryUseCase.getInquiryList(pageable).map(this::fromInternalResponse);
    }

    @Override
    public long getTotalInquiryCount() {
        return inquiryAdminQueryUseCase.getTotalInquiryCount();
    }

    @Override
    public long getWaitingInquiryCount() {
        return inquiryAdminQueryUseCase.getWaitingInquiryCount();
    }

    private AdminInquiryInfo fromInternalResponse(InquiryInternalResponse response) {
        return new AdminInquiryInfo(
                response.inquiryId(),
                response.userId(),
                response.type(),
                response.content(),
                response.answer(),
                response.status(),
                response.createdAt()
        );
    }
}
