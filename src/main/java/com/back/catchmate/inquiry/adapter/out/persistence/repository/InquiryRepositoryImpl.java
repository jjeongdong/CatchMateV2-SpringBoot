package com.back.catchmate.inquiry.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.inquiry.application.port.out.InquiryRepository;
import com.back.catchmate.inquiry.adapter.out.persistence.entity.InquiryEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.back.catchmate.inquiry.adapter.out.persistence.entity.QInquiryEntity.inquiryEntity;
import static com.back.catchmate.user.adapter.out.persistence.entity.QUserEntity.userEntity;

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
    public Page<Inquiry> findAll(Pageable pageable) {
        List<InquiryEntity> entities = jpaQueryFactory
                .selectFrom(inquiryEntity)
                .join(inquiryEntity.user, userEntity).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(inquiryEntity.createdAt.desc())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(inquiryEntity.count())
                .from(inquiryEntity)
                .fetchOne();

        List<Inquiry> inquiries = entities.stream()
                .map(InquiryEntity::toModel)
                .toList();

        return new PageImpl<>(inquiries, pageable, totalCount != null ? totalCount : 0L);
    }

    @Override
    public Page<Inquiry> findAllByUserId(Long userId, Pageable pageable) {
        List<InquiryEntity> entities = jpaQueryFactory
                .selectFrom(inquiryEntity)
                .join(inquiryEntity.user, userEntity).fetchJoin()
                .where(inquiryEntity.user.id.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(inquiryEntity.createdAt.desc())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(inquiryEntity.count())
                .from(inquiryEntity)
                .where(inquiryEntity.user.id.eq(userId))
                .fetchOne();

        List<Inquiry> inquiries = entities.stream()
                .map(InquiryEntity::toModel)
                .toList();

        return new PageImpl<>(inquiries, pageable, totalCount != null ? totalCount : 0L);
    }

    @Override
    public long count() {
        return jpaInquiryRepository.count();
    }

    @Override
    public long countByStatus(com.back.catchmate.inquiry.domain.model.InquiryStatus status) {
        return jpaInquiryRepository.countByStatus(status);
    }
}
