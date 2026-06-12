package com.back.catchmate.user.application.service;

import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.application.dto.command.UploadFile;
import com.back.catchmate.user.application.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.user.application.dto.response.UserAlarmSettingsResponse;
import com.back.catchmate.user.application.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.user.application.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import com.back.catchmate.user.application.dto.response.UserUpdateResponse;
import com.back.catchmate.user.application.port.in.UserUseCase;
import com.back.catchmate.user.application.port.out.ClubFetchPort;
import com.back.catchmate.user.application.port.out.ImageUploaderPort;
import com.back.catchmate.user.application.port.out.UserRepository;
import com.back.catchmate.user.domain.enums.AlarmType;
import com.back.catchmate.user.domain.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepository userRepository;

    private final ImageUploaderPort profileImageUploader;

    private final ClubFetchPort clubFetchPort;

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
            club = clubFetchPort.getClub(command.favoriteClubId());
        }

        String profileImageUrl = null;
        if (uploadFile != null) {
            profileImageUrl = profileImageUploader.upload(
                    uploadFile.originalFilename(),
                    uploadFile.contentType(),
                    uploadFile.inputStream(),
                    uploadFile.size()
            );
        }

        user.updateProfile(command.nickName(), command.watchStyle(), club, profileImageUrl);
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
        user.updateFcmToken(command.fcmToken());
        updateUser(user);
    }

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

    public List<User> getUsers(List<Long> userIds) {
        return userRepository.findAllByIds(userIds);
    }

    public Optional<User> findByProviderId(String providerIdWithProvider) {
        return userRepository.findByProviderId(providerIdWithProvider);
    }

    public List<User> getEventAlarmEnabledUsers() {
        return userRepository.findAllEventAlarmEnabled();
    }

    public Page<User> getUsersByClub(String clubName, Pageable pageable) {
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
