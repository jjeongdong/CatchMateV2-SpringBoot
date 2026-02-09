package com.back.catchmate.inquiry.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum InquiryType {
    ACCOUNT("계정, 로그인 관련"),
    POST("게시글 관련"),
    CHAT("채팅 관련"),
    USER("유저 관련"),
    OTHER("기타");

    @Getter
    private final String description;
}
