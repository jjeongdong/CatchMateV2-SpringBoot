package com.back.catchmate.domain.enroll.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AcceptStatus {
    ACCEPTED("수락 상태"),
    PENDING("대기 상태"),
    REJECTED("거절 상태"),
    ALREADY_ACCEPTED("이미 수락된 상태"),
    ALREADY_REJECTED("이미 거절된 상태"),
    NOT_APPLIED("신청하지 않은 상태");

    private final String description;
}
