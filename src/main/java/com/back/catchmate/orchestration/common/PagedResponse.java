package com.back.catchmate.orchestration.common;

import com.back.catchmate.domain.common.page.DomainPage;
import lombok.Getter;
import java.util.List;

@Getter
public class PagedResponse<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;

    public PagedResponse(DomainPage<?> domainPage, List<T> content) {
        this.content = content;
        this.pageNumber = domainPage.getPageNumber();
        this.totalPages = domainPage.getTotalPages();
        this.totalElements = domainPage.getTotalElements();
        this.hasNext = domainPage.isHasNext();
    }
}
