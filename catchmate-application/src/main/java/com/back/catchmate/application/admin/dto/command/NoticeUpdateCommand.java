package com.back.catchmate.application.admin.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoticeUpdateCommand {
    private String title;
    private String content;
}
