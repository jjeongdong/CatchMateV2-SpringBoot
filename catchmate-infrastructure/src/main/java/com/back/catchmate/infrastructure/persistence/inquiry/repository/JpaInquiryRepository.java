package com.back.catchmate.infrastructure.persistence.inquiry.repository;

import com.back.catchmate.domain.inquiry.model.InquiryStatus;
import com.back.catchmate.infrastructure.persistence.inquiry.entity.InquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInquiryRepository extends JpaRepository<InquiryEntity, Long> {
    long countByStatus(InquiryStatus status);
}
