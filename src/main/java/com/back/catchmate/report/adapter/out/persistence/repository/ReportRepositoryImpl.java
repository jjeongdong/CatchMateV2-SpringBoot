package com.back.catchmate.report.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.report.domain.model.Report;
import com.back.catchmate.report.application.port.out.ReportRepository;
import com.back.catchmate.report.adapter.out.persistence.entity.ReportEntity;
import com.back.catchmate.user.adapter.out.persistence.entity.QUserEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.back.catchmate.report.adapter.out.persistence.entity.QReportEntity.reportEntity;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {
    private final JpaReportRepository jpaReportRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Report save(Report report) {
        ReportEntity entity = ReportEntity.from(report);
        return jpaReportRepository.save(entity).toModel();
    }

    @Override
    public Optional<Report> findById(Long id) {
        return jpaReportRepository.findById(id)
                .map(ReportEntity::toModel);
    }

    @Override
    public Page<Report> findAll(Pageable pageable) {
        QUserEntity reporter = new QUserEntity("reporter");
        QUserEntity reportedUser = new QUserEntity("reportedUser");

        List<ReportEntity> entities = jpaQueryFactory
                .selectFrom(reportEntity)
                .join(reportEntity.reporter, reporter).fetchJoin()      // 신고자 정보
                .join(reportEntity.reportedUser, reportedUser).fetchJoin() // 대상자 정보
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(reportEntity.createdAt.desc())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(reportEntity.count())
                .from(reportEntity)
                .fetchOne();

        List<Report> reports = entities.stream()
                .map(ReportEntity::toModel)
                .toList();

        return new PageImpl<>(reports, pageable, totalCount != null ? totalCount : 0L);
    }

    @Override
    public long count() {
        return jpaReportRepository.count();
    }

    @Override
    public long countByCompleted(boolean completed) {
        return jpaReportRepository.countByCompleted(completed);
    }
}
