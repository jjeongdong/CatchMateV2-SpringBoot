package com.back.catchmate.report.infrastructure;

import static com.back.catchmate.report.domain.QReport.report;

import com.back.catchmate.report.domain.Report;
import com.back.catchmate.report.domain.ReportRepository;
import com.back.catchmate.report.domain.exception.ReportNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {
    private final ReportJpaRepository reportJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Report save(Report newReport) {
        return reportJpaRepository.save(newReport);
    }

    @Override
    public Report getById(Long reportId) {
        return reportJpaRepository.findById(reportId).orElseThrow(ReportNotFoundException::new);
    }

    @Override
    public List<Report> findAllLatest(long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(report)
                .orderBy(report.createdAt.desc(), report.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long count() {
        return reportJpaRepository.count();
    }

    @Override
    public long countByCompleted(boolean completed) {
        return reportJpaRepository.countByCompleted(completed);
    }
}
