package com.back.catchmate.inquiry.adapter.out.persistence.repository;

import com.back.catchmate.inquiry.adapter.out.persistence.entity.InquiryEntity;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInquiryRepository extends JpaRepository<InquiryEntity, Long> {
    long countByStatus(InquiryStatus status);

    Page<InquiryEntity> findAllByUserId(Long userId, Pageable pageable);
}
