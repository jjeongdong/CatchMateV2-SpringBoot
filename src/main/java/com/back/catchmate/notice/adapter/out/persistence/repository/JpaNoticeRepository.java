package com.back.catchmate.notice.adapter.out.persistence.repository;

import com.back.catchmate.notice.adapter.out.persistence.entity.NoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNoticeRepository extends JpaRepository<NoticeEntity, Long> {
}
