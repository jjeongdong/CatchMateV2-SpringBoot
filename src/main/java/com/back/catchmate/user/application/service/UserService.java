package com.back.catchmate.user.application.service;

import com.back.catchmate.auth.application.port.out.TokenProvider;
import com.back.catchmate.auth.application.service.AuthService;
import com.back.catchmate.auth.domain.model.AuthToken;
import com.back.catchmate.club.application.service.ClubService;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.oauth.domain.model.SignupTokenClaims;
import com.back.catchmate.user.application.dto.command.UploadFile;
import com.back.catchmate.user.application.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserRegisterCommand;
import com.back.catchmate.user.application.dto.response.UserAlarmSettingsResponse;
import com.back.catchmate.user.application.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.user.application.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.user.application.dto.response.UserRegisterResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import com.back.catchmate.user.application.dto.response.UserSignupResult;
import com.back.catchmate.user.application.dto.response.UserUpdateResponse;
import com.back.catchmate.user.application.port.in.UserUseCase;
import com.back.catchmate.user.application.port.out.ImageUploaderPort;
import com.back.catchmate.user.application.port.out.UserRepository;
import com.back.catchmate.user.domain.enums.AlarmType;
import com.back.catchmate.user.domain.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final AuthService authService;
    private final ClubService clubService;
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
        User savedUser = createUser(user);

        AuthToken token = authService.createToken(savedUser);
        UserRegisterResponse response = UserRegisterResponse.of(
                savedUser.getId(),
                token.getAccessToken(),
                savedUser.getCreatedAt()
        );
        return new UserSignupResult(response, token.getRefreshToken());
    }

    public UserResponse getUserProfile(Long userId) {
        User user = getUser(userId);
        return UserResponse.from(user);
    }

    public UserResponse getUserProfileById(Long currentUserId, Long targetUserId) {
        User targetUser = getUser(targetUserId);
        return UserResponse.from(targetUser);
    }

    public UserNicknameCheckResponse getUserNicknameAvailability(String nickName) {
        boolean isAvailable = !existsByNickName(nickName);
        return UserNicknameCheckResponse.of(nickName, isAvailable);
    }

    @Transactional
    public UserUpdateResponse updateUserProfile(Long userId, UserProfileUpdateCommand command, UploadFile uploadFile) {
        User user = getUser(userId);

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
        updateUser(user);

        return UserUpdateResponse.from(user);
    }

    @Transactional
    public UserAlarmUpdateResponse updateUserAlarm(Long userId, AlarmType alarmType, boolean isEnabled) {
        User user = getUser(userId);
        user.updateAlarm(alarmType, isEnabled);
        updateUser(user);

        return UserAlarmUpdateResponse.of(alarmType, isEnabled);
    }

    public UserAlarmSettingsResponse getUserAlarmSettings(Long userId) {
        User user = getUser(userId);
        return UserAlarmSettingsResponse.from(user);
    }

    @Transactional
    public void updateUserFcmToken(Long userId, UserFcmTokenUpdateCommand command) {
        User user = getUser(userId);
        user.updateFcmToken(command.getFcmToken());
        updateUser(user);
    }


    private final UserRepository userRepository;

    public User createUser(User user) {
        Optional<User> existingUser = userRepository.findByProviderId(user.getProviderId());
        if (existingUser.isPresent()) {
            throw new BaseException(ErrorCode.USER_ALREADY_EXISTS);
        }

        return userRepository.save(user);
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    public Optional<User> findByProviderId(String providerIdWithProvider) {
        return userRepository.findByProviderId(providerIdWithProvider);
    }

    public List<User> getEventAlarmEnabledUsers() {
        return userRepository.findAllEventAlarmEnabled();
    }

    public DomainPage<User> getUsersByClub(String clubName, DomainPageable pageable) {
        return userRepository.findAllByClubName(clubName, pageable);
    }

    public Map<String, Long> getUserCountByClub() {
        return userRepository.countUsersByClub();
    }

    public Map<String, Long> getUserCountByWatchStyle() {
        return userRepository.countUsersByWatchStyle();
    }

    public boolean existsByNickName(String nickName) {
        return userRepository.existsByNickName(nickName);
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getUserCountByGender(Character gender) {
        return userRepository.countByGender(gender);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }
}
