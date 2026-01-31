package com.back.catchmate.domain.notice.repository;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.notice.model.Notice;

import java.util.Optional;

public interface NoticeRepository {
    Notice save(Notice notice);

    Optional<Notice> findById(Long id);

    DomainPage<Notice> findAll(DomainPageable pageable);

    void delete(Notice notice);
}
