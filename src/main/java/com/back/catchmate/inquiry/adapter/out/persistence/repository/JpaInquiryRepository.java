package com.back.catchmate.inquiry.adapter.out.persistence.repository;

import com.back.catchmate.inquiry.domain.model.InquiryStatus;
import com.back.catchmate.inquiry.adapter.out.persistence.entity.InquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInquiryRepository extends JpaRepository<InquiryEntity, Long> {
    long countByStatus(InquiryStatus status);
}
