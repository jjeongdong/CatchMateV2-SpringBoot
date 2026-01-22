package com.back.catchmate.application.user;

import com.back.catchmate.application.user.dto.UserRegisterCommand;
import com.back.catchmate.application.user.dto.UserRegisterResponse;
import com.back.catchmate.domain.auth.AuthToken;
import com.back.catchmate.domain.auth.service.AuthService;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.club.service.ClubService;
import com.back.catchmate.domain.user.model.Provider;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserUseCase {
    private final UserService userService;
    private final AuthService authService;
    private final ClubService clubService;

    @Transactional
    public UserRegisterResponse register(UserRegisterCommand command) {
        Club club = clubService.getClub(command.getFavoriteClubId());

        User user = User.createUser(
                command.getProvider(),
                command.getProviderIdWithProvider(),
                command.getEmail(),
                command.getNickName(),
                command.getGender(),
                command.getBirthDate(),
                club,
                command.getProfileImageUrl(),
                command.getFcmToken(),
                command.getWatchStyle()
        );
        User savedUser = userService.registerUser(user);

        AuthToken token = authService.issueToken(savedUser.getId());

        return UserRegisterResponse.of(savedUser.getId(), token.getAccessToken(), token.getRefreshToken(), savedUser.getCreatedAt());
    }
}
