package com.back.catchmate.orchestration.user;

import com.back.catchmate.application.auth.service.AuthService;
import com.back.catchmate.application.club.service.ClubService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.auth.model.AuthToken;
import com.back.catchmate.domain.auth.port.TokenProvider;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.oauth.model.SignupTokenClaims;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.port.ImageUploaderPort;
import com.back.catchmate.orchestration.user.dto.command.UploadFile;
import com.back.catchmate.orchestration.user.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.orchestration.user.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.orchestration.user.dto.command.UserRegisterCommand;
import com.back.catchmate.orchestration.user.dto.response.UserAlarmSettingsResponse;
import com.back.catchmate.orchestration.user.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.orchestration.user.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.orchestration.user.dto.response.UserRegisterResponse;
import com.back.catchmate.orchestration.user.dto.response.UserResponse;
import com.back.catchmate.orchestration.user.dto.response.UserSignupResult;
import com.back.catchmate.orchestration.user.dto.response.UserUpdateResponse;
import com.back.catchmate.user.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserOrchestrator {
    private final AuthService authService;
    private final ClubService clubService;
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final ImageUploaderPort profileImageUploader;

    @Transactional
    public UserSignupResult createUser(UserRegisterCommand command) {
        SignupTokenClaims claims = tokenProvider.parseSignupToken(command.getSignupToken());
        Club club = clubService.getClub(command.getFavoriteClubId());

        User user = User.createUser(
                claims.getProvider(),
                claims.getProviderIdWithProvider(),
                claims.getEmail(),
                command.getNickName(),
                command.getGender(),
                command.getBirthDate(),
                club,
                claims.getProfileImageUrl(),
                null,
                command.getWatchStyle()
        );
        User savedUser = userService.createUser(user);

        AuthToken token = authService.createToken(savedUser);
        UserRegisterResponse response = UserRegisterResponse.of(
                savedUser.getId(),
                token.getAccessToken(),
                savedUser.getCreatedAt()
        );
        return new UserSignupResult(response, token.getRefreshToken());
    }

    public UserResponse getUserProfile(Long userId) {
        User user = userService.getUser(userId);
        return UserResponse.from(user);
    }

    public UserResponse getUserProfileById(Long currentUserId, Long targetUserId) {
        User targetUser = userService.getUser(targetUserId);
        return UserResponse.from(targetUser);
    }

    public UserNicknameCheckResponse getUserNicknameAvailability(String nickName) {
        boolean isAvailable = !userService.existsByNickName(nickName);
        return UserNicknameCheckResponse.of(nickName, isAvailable);
    }

    @Transactional
    public UserUpdateResponse updateUserProfile(Long userId, UserProfileUpdateCommand command, UploadFile uploadFile) {
        User user = userService.getUser(userId);

        Club club = null;
        if (command.hasFavoriteClubChange()) {
            club = clubService.getClub(command.getFavoriteClubId());
        }

        String profileImageUrl = null;
        if (uploadFile != null) {
            profileImageUrl = profileImageUploader.upload(
                    uploadFile.getOriginalFilename(),
                    uploadFile.getContentType(),
                    uploadFile.getInputStream(),
                    uploadFile.getSize()
            );
        }

        user.updateProfile(command.getNickName(), command.getWatchStyle(), club, profileImageUrl);
        userService.updateUser(user);

        return UserUpdateResponse.from(user);
    }

    @Transactional
    public UserAlarmUpdateResponse updateUserAlarm(Long userId, AlarmType alarmType, boolean isEnabled) {
        User user = userService.getUser(userId);
        user.updateAlarm(alarmType, isEnabled);
        userService.updateUser(user);

        return UserAlarmUpdateResponse.of(alarmType, isEnabled);
    }

    public UserAlarmSettingsResponse getUserAlarmSettings(Long userId) {
        User user = userService.getUser(userId);
        return UserAlarmSettingsResponse.from(user);
    }

    @Transactional
    public void updateUserFcmToken(Long userId, UserFcmTokenUpdateCommand command) {
        User user = userService.getUser(userId);
        user.updateFcmToken(command.getFcmToken());
        userService.updateUser(user);
    }
}
