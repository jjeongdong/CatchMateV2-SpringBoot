package com.back.catchmate.report.application;

import com.back.catchmate.report.domain.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportQueryApi {
    private final ReportRepository reportRepository;

    /**
     * 전체 신고 수를 센다.
     *
     * @return 신고 수
     */
    @Transactional(readOnly = true)
    public long count() {
        return reportRepository.count();
    }

    /**
     * 아직 처리하지 않은 신고 수를 센다.
     *
     * @return 미처리 신고 수
     */
    @Transactional(readOnly = true)
    public long countPending() {
        return reportRepository.countByCompleted(false);
    }
}
