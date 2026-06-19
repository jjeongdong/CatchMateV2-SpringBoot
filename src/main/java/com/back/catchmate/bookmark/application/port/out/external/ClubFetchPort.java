package com.back.catchmate.bookmark.application.port.out.external;

import com.back.catchmate.bookmark.application.port.out.dto.BookmarkClubInfo;

import java.util.List;

public interface ClubFetchPort {
    List<BookmarkClubInfo> getClubs(List<Long> clubIds);
}
