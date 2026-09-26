package com.back.catchmate.inquiry.infrastructure;

import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.inquiry.domain.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryJpaRepository extends JpaRepository<Inquiry, Long> {
    long countByUserId(Long userId);

    long countByStatus(InquiryStatus status);
}
