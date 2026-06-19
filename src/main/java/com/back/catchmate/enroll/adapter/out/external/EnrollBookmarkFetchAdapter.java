package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.bookmark.application.port.in.BookmarkInternalQueryUseCase;
import com.back.catchmate.enroll.application.port.out.external.BookmarkFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EnrollBookmarkFetchAdapter implements BookmarkFetchPort {
    private final BookmarkInternalQueryUseCase bookmarkInternalQueryUseCase;

    @Override
    public Set<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds) {
        return new HashSet<>(bookmarkInternalQueryUseCase.findBookmarkedBoardIds(userId, boardIds));
    }
}
