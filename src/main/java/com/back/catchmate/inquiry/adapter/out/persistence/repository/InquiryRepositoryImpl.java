package com.back.catchmate.inquiry.adapter.out.persistence.repository;

import com.back.catchmate.inquiry.adapter.out.persistence.entity.InquiryEntity;
import com.back.catchmate.inquiry.application.port.out.persistence.InquiryRepository;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InquiryRepositoryImpl implements InquiryRepository {
    private final JpaInquiryRepository jpaInquiryRepository;

    @Override
    public Inquiry save(Inquiry inquiry) {
        InquiryEntity entity = InquiryEntity.from(inquiry);
        return jpaInquiryRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Inquiry> findById(Long inquiryId) {
        return jpaInquiryRepository.findById(inquiryId).map(InquiryEntity::toDomain);
    }

    @Override
    public Page<Inquiry> findAll(Pageable pageable) {
        return jpaInquiryRepository.findAll(sortByCreatedAtDesc(pageable))
                .map(InquiryEntity::toDomain);
    }

    @Override
    public Page<Inquiry> findAllByUserId(Long userId, Pageable pageable) {
        return jpaInquiryRepository.findAllByUserId(userId, sortByCreatedAtDesc(pageable))
                .map(InquiryEntity::toDomain);
    }

    @Override
    public long count() {
        return jpaInquiryRepository.count();
    }

    @Override
    public long countByStatus(InquiryStatus status) {
        return jpaInquiryRepository.countByStatus(status);
    }

    private PageRequest sortByCreatedAtDesc(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}
