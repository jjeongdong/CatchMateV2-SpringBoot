package com.back.catchmate.common.orchestration;

import org.springframework.data.domain.Page;
import lombok.Getter;
import java.util.List;

@Getter
public class PagedResponse<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;

    public PagedResponse(Page<?> page, List<T> content) {
        this.content = content;
        this.pageNumber = page.getNumber();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.hasNext = page.hasNext();
    }
}
