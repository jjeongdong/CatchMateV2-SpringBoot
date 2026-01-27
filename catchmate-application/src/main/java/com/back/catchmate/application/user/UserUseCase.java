package com.back.catchmate.application.user;

import com.back.catchmate.application.user.dto.UploadFile;
import com.back.catchmate.application.user.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.application.user.dto.command.UserRegisterCommand;
import com.back.catchmate.application.user.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.application.user.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.application.user.dto.response.UserRegisterResponse;
import com.back.catchmate.application.user.dto.response.UserResponse;
import com.back.catchmate.application.user.dto.response.UserUpdateResponse;
import com.back.catchmate.domain.auth.model.AuthToken;
import com.back.catchmate.domain.auth.service.AuthService;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.club.service.ClubService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.enums.AlarmType;

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

        AuthToken token = authService.issueToken(savedUser);

        return UserRegisterResponse.of(savedUser.getId(), token.getAccessToken(), token.getRefreshToken(), savedUser.getCreatedAt());
    }

    public UserResponse getMyProfile(Long userId) {
        User user = userService.getUserById(userId);
        return UserResponse.from(user);
    }

    public UserResponse getOtherUserProfile(Long currentUserId, Long targetUserId) {
        User targetUser = userService.getUserById(targetUserId);
        return UserResponse.from(targetUser);
    }

    public UserNicknameCheckResponse checkNickname(String nickName) {
        boolean isAvailable = !userService.checkNickname(nickName);
        return UserNicknameCheckResponse.of(nickName, isAvailable);
    }

    @Transactional
    public UserUpdateResponse updateProfile(Long userId, UserProfileUpdateCommand command, UploadFile uploadFile) {
        User user = userService.getUserById(userId);

        Club club = null;
        if (command.hasFavoriteClubChange()) {
            club = clubService.getClub(command.getFavoriteClubId());
        }

        // TODO: S3 이미지 업로드 구현
        // String imageUrl = s3UploadService.uploadFile(uploadFile);

        user.updateProfile(command.getNickName(), command.getWatchStyle(), club, "imageUrl");
        userService.updateUser(user);

        return UserUpdateResponse.from(user);
    }

    @Transactional
    public UserAlarmUpdateResponse updateAlarm(Long userId, AlarmType alarmType, boolean isEnabled) {
        User user = userService.getUserById(userId);
        user.updateAlarm(alarmType, isEnabled);
        userService.updateUser(user);

        return UserAlarmUpdateResponse.of(alarmType, isEnabled);
    }
}
