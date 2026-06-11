package com.back.catchmate.inquiry.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;

import java.util.Optional;

public interface InquiryRepository {
    Inquiry save(Inquiry inquiry);

    Optional<Inquiry> findById(Long inquiryId);

    Page<Inquiry> findAll(Pageable pageable);

    Page<Inquiry> findAllByUserId(Long userId, Pageable pageable);

    long count();

    long countByStatus(InquiryStatus status);
}
