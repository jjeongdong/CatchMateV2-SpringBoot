package com.back.catchmate.infrastructure.persistence.bookmark.repository;

import com.back.catchmate.infrastructure.persistence.bookmark.entity.BookmarkEntity;
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

    // N+1 문제 방지를 위해 Fetch Join 사용 (Board와 User 정보를 함께 로딩)
    @Query("SELECT b FROM BookmarkEntity b JOIN FETCH b.board WHERE b.user.id = :userId")
    Page<BookmarkEntity> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b.board.id FROM BookmarkEntity b WHERE b.user.id = :userId AND b.board.id IN :boardIds")
    List<Long> findBookmarkedBoardIds(@Param("userId") Long userId, @Param("boardIds") List<Long> boardIds);
}
