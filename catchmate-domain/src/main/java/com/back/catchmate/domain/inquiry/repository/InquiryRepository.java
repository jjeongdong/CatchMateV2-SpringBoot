package com.back.catchmate.domain.inquiry.repository;

import com.back.catchmate.domain.inquiry.model.Inquiry;
import java.util.Optional;

public interface InquiryRepository {
    Inquiry save(Inquiry inquiry);
    Optional<Inquiry> findById(Long inquiryId);
}
