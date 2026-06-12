package com.back.catchmate.admin.application.dto.response;


public record NoticeActionResponse(
        Long noticeId,
        String message
) {
    public static NoticeActionResponse of(Long noticeId, String message) {
        return new NoticeActionResponse(noticeId, message);
    }
}
