package com.back.catchmate.enroll.application.port.out.external;

import java.util.List;
import java.util.Set;

public interface BookmarkFetchPort {
    Set<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds);
}
