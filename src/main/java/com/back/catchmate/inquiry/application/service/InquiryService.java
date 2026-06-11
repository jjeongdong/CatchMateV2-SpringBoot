package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;
import com.back.catchmate.inquiry.application.port.out.InquiryRepository;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.inquiry.domain.enums.InquiryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private final InquiryRepository inquiryRepository;

    public Inquiry registerInquiry(User user, InquiryType type, String content) {
        Inquiry inquiry = Inquiry.createInquiry(user, type, content);
        return inquiryRepository.save(inquiry);
    }

    public Inquiry getInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BaseException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    public DomainPage<Inquiry> getInquiryList(DomainPageable pageable) {
        return inquiryRepository.findAll(pageable);
    }

    public DomainPage<Inquiry> getInquiryListByUser(Long userId, DomainPageable pageable) {
        return inquiryRepository.findAllByUserId(userId, pageable);
    }

    public long getTotalInquiryCount() {
        return inquiryRepository.count();
    }

    public long getWaitingInquiryCount() {
        return inquiryRepository.countByStatus(InquiryStatus.WAITING);
    }

    public Inquiry updateInquiry(Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }
}
