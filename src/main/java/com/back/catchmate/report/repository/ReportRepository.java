package com.back.catchmate.report.repository;

import com.back.catchmate.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByCompleted(boolean completed);
}
