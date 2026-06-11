package com.back.catchmate.report.application.port.out;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.report.domain.model.Report;

import java.util.Optional;

public interface ReportRepository {
    Report save(Report report);

    Optional<Report> findById(Long id);

    DomainPage<Report> findAll(DomainPageable pageable);

    long count();

    long countByCompleted(boolean completed);
}
