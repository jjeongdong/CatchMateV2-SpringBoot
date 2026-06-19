package com.back.catchmate.admin.application.event;

public record NoticeCreatedEvent(
        Long noticeId,
        String noticeTitle
) {
    public static NoticeCreatedEvent of(Long noticeId, String noticeTitle) {
        return new NoticeCreatedEvent(noticeId, noticeTitle);
    }
}
