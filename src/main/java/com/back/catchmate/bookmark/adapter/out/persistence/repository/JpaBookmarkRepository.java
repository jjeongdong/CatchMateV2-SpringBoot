package com.back.catchmate.bookmark.adapter.out.persistence.repository;

import com.back.catchmate.bookmark.adapter.out.persistence.entity.BookmarkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaBookmarkRepository extends JpaRepository<BookmarkEntity, Long> {
    Optional<BookmarkEntity> findByUserIdAndBoardId(Long userId, Long boardId);

    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    Page<BookmarkEntity> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT b.boardId FROM BookmarkEntity b WHERE b.userId = :userId AND b.boardId IN :boardIds")
    List<Long> findBookmarkedBoardIds(@Param("userId") Long userId, @Param("boardIds") List<Long> boardIds);
}
