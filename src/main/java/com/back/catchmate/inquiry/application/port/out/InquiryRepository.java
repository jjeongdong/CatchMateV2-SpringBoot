package com.back.catchmate.inquiry.application.port.out;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;

import java.util.Optional;

public interface InquiryRepository {
    Inquiry save(Inquiry inquiry);

    Optional<Inquiry> findById(Long inquiryId);

    DomainPage<Inquiry> findAll(DomainPageable pageable);

    DomainPage<Inquiry> findAllByUserId(Long userId, DomainPageable pageable);

    long count();

    long countByStatus(InquiryStatus status);
}
