package com.back.catchmate.report.adapter.out.persistence.repository;

import com.back.catchmate.report.adapter.out.persistence.entity.ReportEntity;
import com.back.catchmate.report.application.port.out.persistence.ReportRepository;
import com.back.catchmate.report.domain.model.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {
    private final JpaReportRepository jpaReportRepository;

    @Override
    public Report save(Report report) {
        ReportEntity entity = ReportEntity.from(report);
        return jpaReportRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Report> findById(Long id) {
        return jpaReportRepository.findById(id)
                .map(ReportEntity::toDomain);
    }

    @Override
    public Page<Report> findAll(Pageable pageable) {
        PageRequest sortedPageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return jpaReportRepository.findAll(sortedPageRequest)
                .map(ReportEntity::toDomain);
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
