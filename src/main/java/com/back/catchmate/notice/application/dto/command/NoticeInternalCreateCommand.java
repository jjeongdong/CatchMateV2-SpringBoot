package com.back.catchmate.notice.application.dto.command;

public record NoticeInternalCreateCommand(
        Long writerId,
        String title,
        String content
) {
}
