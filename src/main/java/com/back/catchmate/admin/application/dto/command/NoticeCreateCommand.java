package com.back.catchmate.admin.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NoticeCreateCommand {
    private String title;
    private String content;
}
