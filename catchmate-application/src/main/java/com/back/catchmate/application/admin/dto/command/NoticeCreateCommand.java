package com.back.catchmate.application.admin.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeCreateCommand {
    private String title;
    private String content;
}
