package com.back.catchmate.infrastructure.persistence.inquiry.repository;

import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.inquiry.repository.InquiryRepository;
import com.back.catchmate.infrastructure.persistence.inquiry.entity.InquiryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InquiryRepositoryImpl implements InquiryRepository {
    private final JpaInquiryRepository jpaInquiryRepository;

    @Override
    public Inquiry save(Inquiry inquiry) {
        InquiryEntity entity = InquiryEntity.from(inquiry);
        return jpaInquiryRepository.save(entity).toModel();
    }

    @Override
    public Optional<Inquiry> findById(Long inquiryId) {
        return jpaInquiryRepository.findById(inquiryId).map(InquiryEntity::toModel);
    }
}
