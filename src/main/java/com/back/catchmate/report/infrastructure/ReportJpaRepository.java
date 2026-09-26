package com.back.catchmate.report.infrastructure;

import com.back.catchmate.report.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportJpaRepository extends JpaRepository<Report, Long> {
    long countByCompleted(boolean completed);
}
