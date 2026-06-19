package com.back.catchmate.bookmark.application.service;

import com.back.catchmate.bookmark.application.port.in.BookmarkInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookmarkInternalQueryService implements BookmarkInternalQueryUseCase {
    private final BookmarkReader bookmarkReader;

    @Override
    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkReader.isBookmarked(userId, boardId);
    }

    @Override
    public List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) {
            return List.of();
        }
        return bookmarkReader.findBookmarkedBoardIds(userId, boardIds);
    }
}
