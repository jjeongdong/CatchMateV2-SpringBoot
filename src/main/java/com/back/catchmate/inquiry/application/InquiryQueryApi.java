package com.back.catchmate.inquiry.application;

import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.inquiry.domain.InquiryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryQueryApi {
    private final InquiryRepository inquiryRepository;

    /**
     * 전체 문의 수를 센다 (관리자 대시보드용).
     *
     * @return 전체 문의 수
     */
    @Transactional(readOnly = true)
    public long count() {
        return inquiryRepository.count();
    }

    /**
     * 답변 대기 중인 문의 수를 센다 (관리자 대시보드용).
     *
     * @return 답변 대기 문의 수
     */
    @Transactional(readOnly = true)
    public long countWaiting() {
        return inquiryRepository.countByStatus(InquiryStatus.WAITING);
    }
}
