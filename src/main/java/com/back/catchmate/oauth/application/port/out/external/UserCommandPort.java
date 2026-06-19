package com.back.catchmate.oauth.application.port.out.external;

import com.back.catchmate.oauth.application.dto.command.RegisterUserCommand;
import com.back.catchmate.oauth.application.dto.response.CreatedUserSummary;

public interface UserCommandPort {
    CreatedUserSummary createUser(RegisterUserCommand command);
}
