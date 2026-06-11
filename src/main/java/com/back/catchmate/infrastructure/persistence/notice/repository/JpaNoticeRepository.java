package com.back.catchmate.infrastructure.persistence.notice.repository;

import com.back.catchmate.infrastructure.persistence.notice.entity.NoticeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JpaNoticeRepository extends JpaRepository<NoticeEntity, Long> {
    // N+1 문제 해결을 위한 Fetch Join
    @Query("SELECT n FROM NoticeEntity n JOIN FETCH n.writer")
    Page<NoticeEntity> findAllWithWriter(Pageable pageable);

    @Query("SELECT n FROM NoticeEntity n JOIN FETCH n.writer WHERE n.id = :id")
    Optional<NoticeEntity> findByIdWithWriter(Long id);
}
