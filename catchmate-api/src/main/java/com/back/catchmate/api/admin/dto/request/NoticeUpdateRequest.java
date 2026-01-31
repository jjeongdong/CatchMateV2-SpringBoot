package com.back.catchmate.api.admin.dto.request;

import com.back.catchmate.application.admin.dto.command.NoticeUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeUpdateRequest {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    public NoticeUpdateCommand toCommand() {
        return NoticeUpdateCommand.builder()
                .title(this.title)
                .content(this.content)
                .build();
    }
}
