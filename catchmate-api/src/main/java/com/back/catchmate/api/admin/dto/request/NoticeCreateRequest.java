package com.back.catchmate.api.admin.dto.request;

import com.back.catchmate.application.admin.dto.command.NoticeCreateCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeCreateRequest {
    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    public NoticeCreateCommand toCommand() {
        return NoticeCreateCommand.builder()
                .title(this.title)
                .content(this.content)
                .build();
    }
}
