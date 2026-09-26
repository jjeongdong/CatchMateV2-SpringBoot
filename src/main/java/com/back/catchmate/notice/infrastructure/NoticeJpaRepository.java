package com.back.catchmate.notice.infrastructure;

import com.back.catchmate.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeJpaRepository extends JpaRepository<Notice, Long> {}
