package com.back.catchmate.report.adapter.out.persistence.repository;

import com.back.catchmate.report.adapter.out.persistence.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReportRepository extends JpaRepository<ReportEntity, Long> {
    long countByCompleted(boolean completed);
}
