package com.back.catchmate.bookmark.application;

import com.back.catchmate.bookmark.domain.BookmarkRepository;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkQueryApi {
    private final BookmarkRepository bookmarkRepository;

    /**
     * 사용자가 게시글을 찜했는지 확인한다.
     *
     * @param userId  사용자 ID
     * @param boardId 게시글 ID
     * @return 찜했으면 true
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }

    /**
     * 주어진 게시글 중 사용자가 찜한 게시글 ID 를 고른다.
     *
     * @param userId   사용자 ID
     * @param boardIds 확인할 게시글 ID 들
     * @return 찜한 게시글 ID 집합, 입력이 비면 조회 없이 빈 집합
     */
    @Transactional(readOnly = true)
    public Set<Long> getBookmarkedBoardIds(Long userId, Collection<Long> boardIds) {
        if (boardIds.isEmpty()) {
            return Set.of();
        }
        return bookmarkRepository.findBookmarkedBoardIds(userId, boardIds);
    }
}
