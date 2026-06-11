package com.back.catchmate.infrastructure.persistence.report.repository;

import com.back.catchmate.infrastructure.persistence.report.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReportRepository extends JpaRepository<ReportEntity, Long> {
    long countByCompleted(boolean completed);
}
