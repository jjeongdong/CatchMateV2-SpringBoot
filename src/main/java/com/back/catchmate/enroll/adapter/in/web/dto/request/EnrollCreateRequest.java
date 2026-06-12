package com.back.catchmate.enroll.adapter.in.web.dto.request;

import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;

public record EnrollCreateRequest(
        String description
) {
    public EnrollCreateCommand toCommand(Long userId, Long boardId) {
        return EnrollCreateCommand.builder()
                .userId(userId)
                .boardId(boardId)
                .description(description)
                .build();
    }
}
