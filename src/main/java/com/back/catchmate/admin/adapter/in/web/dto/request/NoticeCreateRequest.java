package com.back.catchmate.admin.adapter.in.web.dto.request;

import com.back.catchmate.admin.application.dto.command.NoticeCreateCommand;
import jakarta.validation.constraints.NotBlank;

public record NoticeCreateRequest(
        @NotBlank(message = "제목을 입력해주세요.") String title,
        @NotBlank(message = "내용을 입력해주세요.") String content
) {
    public NoticeCreateCommand toCommand() {
        return new NoticeCreateCommand(
                this.title,
                this.content
        );
    }
}
