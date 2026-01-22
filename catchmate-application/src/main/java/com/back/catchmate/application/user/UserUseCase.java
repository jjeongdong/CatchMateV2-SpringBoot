package com.back.catchmate.application.user;

import com.back.catchmate.application.user.dto.UserRegisterCommand;
import com.back.catchmate.application.user.dto.UserRegisterResponse;
import com.back.catchmate.application.user.dto.UserResponse;
import com.back.catchmate.domain.auth.AuthToken;
import com.back.catchmate.domain.auth.service.AuthService;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.club.service.ClubService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
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

    public UserResponse getMyProfile(Long userId) {
        User user = userService.getUserById(userId);
        return UserResponse.from(user);
    }

    public UserResponse getOtherUserProfile(Long currentUserId, Long targetUserId) {
        User targetUser = userService.getUserById(targetUserId);
        boolean isMe = currentUserId.equals(targetUserId);

        return UserResponse.of(targetUser, isMe);
    }
}
