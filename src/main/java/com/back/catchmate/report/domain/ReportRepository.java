package com.back.catchmate.report.domain;

import java.util.List;

public interface ReportRepository {
    Report save(Report report);

    Report getById(Long reportId);

    /** 최신 접수순 (같으면 id 내림차순). */
    List<Report> findAllLatest(long offset, int limit);

    long count();

    long countByCompleted(boolean completed);
}
