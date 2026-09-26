package com.back.catchmate.notice.infrastructure;

import static com.back.catchmate.notice.domain.QNotice.notice;

import com.back.catchmate.notice.domain.Notice;
import com.back.catchmate.notice.domain.NoticeRepository;
import com.back.catchmate.notice.domain.exception.NoticeNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepository {
    private final NoticeJpaRepository noticeJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Notice save(Notice newNotice) {
        return noticeJpaRepository.save(newNotice);
    }

    @Override
    public Notice getById(Long noticeId) {
        return noticeJpaRepository.findById(noticeId).orElseThrow(NoticeNotFoundException::new);
    }

    @Override
    public void delete(Notice target) {
        noticeJpaRepository.delete(target);
    }

    @Override
    public List<Notice> findAllLatest(long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(notice)
                .orderBy(notice.createdAt.desc(), notice.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long count() {
        return noticeJpaRepository.count();
    }
}
