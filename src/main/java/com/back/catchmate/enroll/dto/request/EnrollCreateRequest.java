package com.back.catchmate.enroll.dto.request;

import com.back.catchmate.enroll.dto.command.EnrollCreateCommand;

public record EnrollCreateRequest(
        String description
) {
    public EnrollCreateCommand toCommand(Long userId, Long boardId) {
        return new EnrollCreateCommand(
                userId,
                boardId,
                description
        );
    }
}
