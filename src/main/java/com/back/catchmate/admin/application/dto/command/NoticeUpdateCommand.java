package com.back.catchmate.admin.application.dto.command;


public record NoticeUpdateCommand(
        String title,
        String content
) {
}
