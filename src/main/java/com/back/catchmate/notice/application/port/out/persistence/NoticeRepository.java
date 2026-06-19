package com.back.catchmate.notice.application.port.out.persistence;

import com.back.catchmate.notice.domain.model.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NoticeRepository {
    Notice save(Notice notice);

    Optional<Notice> findById(Long id);

    Page<Notice> findAll(Pageable pageable);

    void delete(Notice notice);
}
