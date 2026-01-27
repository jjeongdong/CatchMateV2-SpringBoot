package com.back.catchmate.domain.common;

import lombok.Getter;

@Getter
public class DomainPageable {
    private final int page;
    private final int size;
    private final int offset;

    public DomainPageable(int page, int size) {
        this.page = page;
        this.size = size;
        this.offset = page * size;
    }

    public static DomainPageable of(int page, int size) {
        return new DomainPageable(page, size);
    }
}
