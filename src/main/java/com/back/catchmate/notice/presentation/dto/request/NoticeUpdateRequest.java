package com.back.catchmate.notice.presentation.dto.request;

import com.back.catchmate.notice.application.dto.command.NoticeUpdateCommand;
import jakarta.validation.constraints.NotBlank;

public record NoticeUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.") String title, @NotBlank(message = "내용은 필수입니다.") String content) {
    public NoticeUpdateCommand toCommand() {
        return new NoticeUpdateCommand(title, content);
    }
}
