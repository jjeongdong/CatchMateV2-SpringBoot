package com.back.catchmate.admin.application.dto.response;


public record AdminNoticeActionResponse(
        Long noticeId,
        String message
) {
    public static AdminNoticeActionResponse of(Long noticeId, String message) {
        return new AdminNoticeActionResponse(noticeId, message);
    }
}
