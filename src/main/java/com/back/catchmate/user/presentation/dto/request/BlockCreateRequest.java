package com.back.catchmate.user.presentation.dto.request;

import com.back.catchmate.user.application.dto.command.BlockCreateCommand;
import jakarta.validation.constraints.NotNull;

public record BlockCreateRequest(@NotNull(message = "blockedUserId는 필수 값입니다.") Long blockedUserId) {
    public BlockCreateCommand toCommand() {
        return new BlockCreateCommand(blockedUserId);
    }
}
