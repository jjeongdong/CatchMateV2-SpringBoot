package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.inquiry.application.dto.response.InquiryInternalResponse;
import com.back.catchmate.inquiry.application.port.in.InquiryAdminQueryUseCase;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryInternalQueryService implements InquiryAdminQueryUseCase {
    private final InquiryReader inquiryReader;

    @Override
    public InquiryInternalResponse getInquiry(Long inquiryId) {
        return toInternalResponse(inquiryReader.getInquiry(inquiryId));
    }

    @Override
    public Page<InquiryInternalResponse> getInquiryList(Pageable pageable) {
        return inquiryReader.getInquiryList(pageable).map(this::toInternalResponse);
    }

    @Override
    public Page<InquiryInternalResponse> getInquiryListByUser(Long userId, Pageable pageable) {
        return inquiryReader.getInquiryListByUser(userId, pageable).map(this::toInternalResponse);
    }

    @Override
    public long getTotalInquiryCount() {
        return inquiryReader.getTotalInquiryCount();
    }

    @Override
    public long getWaitingInquiryCount() {
        return inquiryReader.getWaitingInquiryCount();
    }

    private InquiryInternalResponse toInternalResponse(Inquiry inquiry) {
        return new InquiryInternalResponse(
                inquiry.getId(),
                inquiry.getUserId(),
                inquiry.getType().name(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt()
        );
    }
}
