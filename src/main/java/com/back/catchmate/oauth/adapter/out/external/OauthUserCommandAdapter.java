package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.oauth.application.dto.command.RegisterUserCommand;
import com.back.catchmate.oauth.application.dto.response.CreatedUserSummary;
import com.back.catchmate.oauth.application.port.out.external.UserCommandPort;
import com.back.catchmate.user.application.dto.command.CreateUserCommand;
import com.back.catchmate.user.application.dto.response.CreatedUserResponse;
import com.back.catchmate.user.application.port.in.UserInternalCommandUseCase;
import com.back.catchmate.oauth.domain.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthUserCommandAdapter implements UserCommandPort {
    private final UserInternalCommandUseCase userInternalCommandUseCase;

    @Override
    public CreatedUserSummary createUser(RegisterUserCommand command) {
        CreateUserCommand createUserCommand = new CreateUserCommand(
                command.provider(),
                command.providerIdWithProvider(),
                command.email(),
                command.nickName(),
                command.gender(),
                command.birthDate(),
                command.favoriteClubId(),
                command.profileImageUrl(),
                command.watchStyle()
        );

        CreatedUserResponse response = userInternalCommandUseCase.createUser(createUserCommand);
        return new CreatedUserSummary(response.userId(), response.authority(), response.createdAt());
    }
}
