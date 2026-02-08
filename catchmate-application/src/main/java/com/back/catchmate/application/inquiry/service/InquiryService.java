package com.back.catchmate.application.inquiry.service;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.inquiry.repository.InquiryRepository;
import com.back.catchmate.domain.user.model.User;
import error.ErrorCode;
import error.exception.BaseException;
import inquiry.enums.InquiryType;
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

    public long getTotalInquiryCount() {
        return inquiryRepository.count();
    }

    public Inquiry updateInquiry(Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }
}
