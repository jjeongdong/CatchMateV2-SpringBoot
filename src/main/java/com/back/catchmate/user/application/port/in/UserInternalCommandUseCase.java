package com.back.catchmate.user.application.port.in;

import com.back.catchmate.user.application.dto.command.CreateUserCommand;
import com.back.catchmate.user.application.dto.response.CreatedUserResponse;

public interface UserInternalCommandUseCase {
    CreatedUserResponse createUser(CreateUserCommand command);

    void markUserAsReported(Long userId);

    void clearFcmToken(Long userId);
}
