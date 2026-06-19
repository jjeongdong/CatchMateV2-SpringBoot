package com.back.catchmate.enroll.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AcceptStatus {
    ACCEPTED("수락 상태"),
    PENDING("대기 상태"),
    REJECTED("거절 상태");

    private final String description;
}
