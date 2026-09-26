package com.back.catchmate.inquiry.domain;

import java.util.List;

public interface InquiryRepository {
    Inquiry save(Inquiry inquiry);

    Inquiry getById(Long inquiryId);

    /** 최신 작성순 (같으면 id 내림차순). */
    List<Inquiry> findAllLatest(long offset, int limit);

    long count();

    /** 최신 작성순 (같으면 id 내림차순). */
    List<Inquiry> findAllLatestByUserId(Long userId, long offset, int limit);

    long countByUserId(Long userId);

    long countByStatus(InquiryStatus status);

    /** 답변이 있는 답변 완료 문의를 최신 작성순으로 최대 limit 건. */
    List<Inquiry> findAllLatestAnswered(int limit);
}
