package com.back.catchmate.enroll.dto.command;

public record EnrollCreateCommand(Long userId, Long boardId, String description) {}
