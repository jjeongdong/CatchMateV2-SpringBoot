package com.back.catchmate.admin.application.dto.command;


public record NoticeCreateCommand(
        String title,
        String content
) {
}
