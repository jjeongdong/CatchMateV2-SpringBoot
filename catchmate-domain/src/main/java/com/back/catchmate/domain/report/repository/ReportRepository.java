package com.back.catchmate.domain.report.repository;

import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.report.model.Report;

import java.util.Optional;

public interface ReportRepository {
    Report save(Report report);
    DomainPage<Report> findAll(DomainPageable pageable);
    Optional<Report> findById(Long reportId);

    // 대시보드
    long count();
}
