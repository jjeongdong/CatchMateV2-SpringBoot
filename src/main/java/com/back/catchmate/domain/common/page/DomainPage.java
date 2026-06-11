package com.back.catchmate.domain.common.page;

import lombok.Getter;
import java.util.List;

@Getter
public class DomainPage<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;

    public DomainPage(List<T> content, int pageNumber, int pageSize, long totalElements) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = (pageSize == 0) ? 1 : (int) Math.ceil((double) totalElements / (double) pageSize);
        this.hasNext = pageNumber + 1 < totalPages;
    }
}
