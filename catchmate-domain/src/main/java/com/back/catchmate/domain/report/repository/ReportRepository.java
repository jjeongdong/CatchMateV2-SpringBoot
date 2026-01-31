package com.back.catchmate.domain.report.repository;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.report.model.Report;

import java.util.Optional;

public interface ReportRepository {
    Report save(Report report);

    Optional<Report> findById(Long id);

    DomainPage<Report> findAll(DomainPageable pageable);

    long count();
}
