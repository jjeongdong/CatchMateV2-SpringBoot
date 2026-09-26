package com.back.catchmate.enroll.presentation.dto.request;

import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;

public record EnrollCreateRequest(String description) {
    public EnrollCreateCommand toCommand() {
        return new EnrollCreateCommand(description);
    }
}
