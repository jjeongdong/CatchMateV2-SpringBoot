package com.back.catchmate.inquiry.repository;

import com.back.catchmate.inquiry.entity.Inquiry;
import com.back.catchmate.inquiry.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    long countByStatus(InquiryStatus status);

    Page<Inquiry> findAllByUserId(Long userId, Pageable pageable);
}
