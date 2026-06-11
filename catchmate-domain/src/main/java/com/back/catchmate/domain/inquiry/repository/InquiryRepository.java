package com.back.catchmate.domain.inquiry.repository;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.inquiry.model.InquiryStatus;

import java.util.Optional;

public interface InquiryRepository {
    Inquiry save(Inquiry inquiry);

    Optional<Inquiry> findById(Long inquiryId);

    DomainPage<Inquiry> findAll(DomainPageable pageable);

    DomainPage<Inquiry> findAllByUserId(Long userId, DomainPageable pageable);

    long count();

    long countByStatus(InquiryStatus status);
}
