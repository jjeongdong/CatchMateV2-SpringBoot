package com.back.catchmate.inquiry.infrastructure;

import static com.back.catchmate.inquiry.domain.QInquiry.inquiry;

import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.inquiry.domain.InquiryStatus;
import com.back.catchmate.inquiry.domain.exception.InquiryNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InquiryRepositoryImpl implements InquiryRepository {
    private final InquiryJpaRepository inquiryJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Inquiry save(Inquiry newInquiry) {
        return inquiryJpaRepository.save(newInquiry);
    }

    @Override
    public Inquiry getById(Long inquiryId) {
        return inquiryJpaRepository.findById(inquiryId).orElseThrow(InquiryNotFoundException::new);
    }

    @Override
    public List<Inquiry> findAllLatest(long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(inquiry)
                .orderBy(inquiry.createdAt.desc(), inquiry.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long count() {
        return inquiryJpaRepository.count();
    }

    @Override
    public List<Inquiry> findAllLatestByUserId(Long userId, long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(inquiry)
                .where(inquiry.userId.eq(userId))
                .orderBy(inquiry.createdAt.desc(), inquiry.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByUserId(Long userId) {
        return inquiryJpaRepository.countByUserId(userId);
    }

    @Override
    public long countByStatus(InquiryStatus status) {
        return inquiryJpaRepository.countByStatus(status);
    }

    @Override
    public List<Inquiry> findAllLatestAnswered(int limit) {
        return jpaQueryFactory
                .selectFrom(inquiry)
                .where(inquiry.status.eq(InquiryStatus.ANSWERED), inquiry.answer.isNotNull())
                .orderBy(inquiry.createdAt.desc(), inquiry.id.desc())
                .limit(limit)
                .fetch();
    }
}
