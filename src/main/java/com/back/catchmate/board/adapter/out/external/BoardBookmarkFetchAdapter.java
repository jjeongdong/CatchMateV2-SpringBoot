package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.BookmarkFetchPort;
import com.back.catchmate.bookmark.application.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardBookmarkFetchAdapter implements BookmarkFetchPort {
    private final BookmarkService bookmarkService;

    @Override
    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkService.isBookmarked(userId, boardId);
    }

    @Override
    public List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds) {
        return bookmarkService.findBookmarkedBoardIds(userId, boardIds);
    }
}
