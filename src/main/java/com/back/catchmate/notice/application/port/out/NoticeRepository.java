package com.back.catchmate.notice.application.port.out;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.notice.domain.model.Notice;

import java.util.Optional;

public interface NoticeRepository {
    Notice save(Notice notice);

    Optional<Notice> findById(Long id);

    DomainPage<Notice> findAll(DomainPageable pageable);

    void delete(Notice notice);
}
