package com.back.catchmate.bookmark.infrastructure;

import com.back.catchmate.bookmark.domain.Bookmark;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkJpaRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    long countByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Bookmark b WHERE b.userId = :userId AND b.boardId = :boardId")
    int deleteByUserIdAndBoardId(@Param("userId") Long userId, @Param("boardId") Long boardId);

    @Query("SELECT b.boardId FROM Bookmark b WHERE b.userId = :userId AND b.boardId IN :boardIds")
    List<Long> findBookmarkedBoardIds(@Param("userId") Long userId, @Param("boardIds") Collection<Long> boardIds);
}
