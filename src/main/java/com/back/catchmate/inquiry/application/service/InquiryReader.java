package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.inquiry.application.port.out.persistence.InquiryRepository;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryReader {
    private final InquiryRepository inquiryRepository;

    public Inquiry getInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BaseException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    public Page<Inquiry> getInquiryList(Pageable pageable) {
        return inquiryRepository.findAll(pageable);
    }

    public Page<Inquiry> getInquiryListByUser(Long userId, Pageable pageable) {
        return inquiryRepository.findAllByUserId(userId, pageable);
    }

    public long getTotalInquiryCount() {
        return inquiryRepository.count();
    }

    public long getWaitingInquiryCount() {
        return inquiryRepository.countByStatus(InquiryStatus.WAITING);
    }
}
