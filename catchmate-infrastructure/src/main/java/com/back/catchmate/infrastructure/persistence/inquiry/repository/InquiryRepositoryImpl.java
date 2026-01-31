package com.back.catchmate.infrastructure.persistence.inquiry.repository;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.inquiry.repository.InquiryRepository;
import com.back.catchmate.infrastructure.persistence.inquiry.entity.InquiryEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.back.catchmate.infrastructure.persistence.inquiry.entity.QInquiryEntity.inquiryEntity;
import static com.back.catchmate.infrastructure.persistence.user.entity.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class InquiryRepositoryImpl implements InquiryRepository {
    private final JpaInquiryRepository jpaInquiryRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Inquiry save(Inquiry inquiry) {
        InquiryEntity entity = InquiryEntity.from(inquiry);
        return jpaInquiryRepository.save(entity).toModel();
    }

    @Override
    public Optional<Inquiry> findById(Long inquiryId) {
        return jpaInquiryRepository.findById(inquiryId).map(InquiryEntity::toModel);
    }

    @Override
    public DomainPage<Inquiry> findAll(DomainPageable pageable) {
        List<InquiryEntity> entities = jpaQueryFactory
                .selectFrom(inquiryEntity)
                .join(inquiryEntity.user, userEntity).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getSize())
                .orderBy(inquiryEntity.createdAt.desc())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(inquiryEntity.count())
                .from(inquiryEntity)
                .fetchOne();

        List<Inquiry> inquiries = entities.stream()
                .map(InquiryEntity::toModel)
                .toList();

        return new DomainPage<>(
                inquiries,
                pageable.getPage(),
                pageable.getSize(),
                totalCount != null ? totalCount : 0L
        );
    }

    @Override
    public long count() {
        return jpaInquiryRepository.count();
    }
}
