package com.back.catchmate.orchestration.bookmark.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookmarkUpdateResponse {
    private Long boardId;
    private boolean bookmarked;

    public static BookmarkUpdateResponse of(Long boardId, boolean bookmarked) {
        return new BookmarkUpdateResponse(boardId, bookmarked);
    }
}
