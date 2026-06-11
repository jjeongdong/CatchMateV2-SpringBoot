package com.back.catchmate.enroll.adapter.in.web.dto.request;

import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollCreateRequest {
    private String description;

    public EnrollCreateCommand toCommand(Long userId, Long boardId) {
        return EnrollCreateCommand.builder()
                .userId(userId)
                .boardId(boardId)
                .description(description)
                .build();
    }
}
