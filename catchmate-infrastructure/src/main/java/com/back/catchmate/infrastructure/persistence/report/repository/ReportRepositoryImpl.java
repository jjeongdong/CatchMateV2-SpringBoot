package com.back.catchmate.infrastructure.persistence.report.repository;

import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.domain.report.repository.ReportRepository;
import com.back.catchmate.infrastructure.persistence.report.entity.ReportEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.QUserEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.back.catchmate.infrastructure.persistence.report.entity.QReportEntity.reportEntity;

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
    public DomainPage<Report> findAll(DomainPageable pageable) {
        QUserEntity reporter = new QUserEntity("reporter");
        QUserEntity reportedUser = new QUserEntity("reportedUser");

        List<ReportEntity> entities = jpaQueryFactory
                .selectFrom(reportEntity)
                .join(reportEntity.reporter, reporter).fetchJoin()      // 신고자 정보
                .join(reportEntity.reportedUser, reportedUser).fetchJoin() // 대상자 정보
                .offset(pageable.getOffset())
                .limit(pageable.getSize())
                .orderBy(reportEntity.createdAt.desc())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(reportEntity.count())
                .from(reportEntity)
                .fetchOne();

        List<Report> reports = entities.stream()
                .map(ReportEntity::toModel)
                .toList();

        return new DomainPage<>(
                reports,
                pageable.getPage(),
                pageable.getSize(),
                totalCount != null ? totalCount : 0L
        );
    }

    @Override
    public Optional<Report> findById(Long reportId) {
        return jpaReportRepository.findById(reportId)
                .map(ReportEntity::toModel);
    }

    @Override
    public long count() {
        return jpaReportRepository.count();
    }
}
