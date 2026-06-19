package com.back.catchmate.report.application.port.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.report.domain.model.Report;

import java.util.Optional;

public interface ReportRepository {
    Report save(Report report);

    Optional<Report> findById(Long id);

    Page<Report> findAll(Pageable pageable);

    long count();

    long countByCompleted(boolean completed);
}
