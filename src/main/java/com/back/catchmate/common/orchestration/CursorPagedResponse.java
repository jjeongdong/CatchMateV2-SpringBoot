package com.back.catchmate.common.orchestration;

import com.back.catchmate.common.page.CursorPage;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class CursorPagedResponse<T> {
    private final List<T> content;
    private final boolean hasNext;
    private final Long nextCursorId;
    private final String nextCursorDateTime;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public CursorPagedResponse(CursorPage<?> cursorPage, List<T> content) {
        this.content = content;
        this.hasNext = cursorPage.isHasNext();
        this.nextCursorId = cursorPage.getNextCursorId();
        this.nextCursorDateTime = cursorPage.getNextCursorDateTime() != null
                ? cursorPage.getNextCursorDateTime().format(FORMATTER)
                : null;
    }
}
