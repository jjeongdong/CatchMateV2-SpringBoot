package com.back.catchmate.notice.domain.event;

public record NoticeCreatedEvent(Long noticeId, String noticeTitle) {}
