package com.back.catchmate.notice.application.dto.command;

public record NoticeInternalUpdateCommand(
        Long noticeId,
        String title,
        String content
) {
}
