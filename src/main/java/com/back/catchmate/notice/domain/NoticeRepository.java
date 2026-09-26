package com.back.catchmate.notice.domain;

import java.util.List;

public interface NoticeRepository {
    Notice save(Notice notice);

    Notice getById(Long noticeId);

    void delete(Notice notice);

    /** 최신 작성순 (같으면 id 내림차순). */
    List<Notice> findAllLatest(long offset, int limit);

    long count();
}
