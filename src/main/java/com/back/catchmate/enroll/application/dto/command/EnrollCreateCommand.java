package com.back.catchmate.enroll.application.dto.command;


public record EnrollCreateCommand(
        Long userId,
        Long boardId,
        String description
) {
}
