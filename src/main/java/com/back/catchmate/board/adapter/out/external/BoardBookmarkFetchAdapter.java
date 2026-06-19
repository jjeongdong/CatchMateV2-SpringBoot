package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.external.BookmarkFetchPort;
import com.back.catchmate.bookmark.application.port.in.BookmarkInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardBookmarkFetchAdapter implements BookmarkFetchPort {
    private final BookmarkInternalQueryUseCase bookmarkInternalQueryUseCase;

    @Override
    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkInternalQueryUseCase.isBookmarked(userId, boardId);
    }

    @Override
    public List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds) {
        return bookmarkInternalQueryUseCase.findBookmarkedBoardIds(userId, boardIds);
    }
}
