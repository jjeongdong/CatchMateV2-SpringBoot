package com.back.catchmate.domain.common.page;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CursorPage<T> {
    private final List<T> content;
    private final boolean hasNext;
    private final Long nextCursorId;
    private final LocalDateTime nextCursorDateTime;

    public CursorPage(List<T> content, boolean hasNext,
                      Long nextCursorId, LocalDateTime nextCursorDateTime) {
        this.content = content;
        this.hasNext = hasNext;
        this.nextCursorId = nextCursorId;
        this.nextCursorDateTime = nextCursorDateTime;
    }
}
