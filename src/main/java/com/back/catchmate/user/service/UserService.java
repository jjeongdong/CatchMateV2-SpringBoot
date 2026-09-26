package com.back.catchmate.user.service;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.user.dto.command.CreateUserCommand;
import com.back.catchmate.user.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.user.dto.request.UserProfileUpdateRequest;
import com.back.catchmate.user.dto.response.CreatedUserResponse;
import com.back.catchmate.user.dto.response.UserAlarmSettingsResponse;
import com.back.catchmate.user.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.user.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.user.dto.response.UserResponse;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.dto.response.UserUpdateResponse;
import com.back.catchmate.user.entity.User;
import com.back.catchmate.user.entity.UserAlarmType;
import com.back.catchmate.user.infra.S3ImageUploader;
import com.back.catchmate.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ClubService clubService;
    private final S3ImageUploader s3ImageUploader;

    // 컨트롤러용
    public UserResponse getUserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        ClubSummary club = user.getClubId() != null ? clubService.getClubSummary(user.getClubId()) : null;
        return UserResponse.from(user, club);
    }

    public UserResponse getUserProfileById(Long currentUserId, Long targetUserId) {
        User targetUser = getUserOrThrow(targetUserId);
        ClubSummary club = targetUser.getClubId() != null ? clubService.getClubSummary(targetUser.getClubId()) : null;
        return UserResponse.from(targetUser, club);
    }

    public UserNicknameCheckResponse getUserNicknameAvailability(String nickName) {
        boolean isAvailable = !userRepository.existsByNickName(nickName);
        return UserNicknameCheckResponse.of(nickName, isAvailable);
    }

    public UserAlarmSettingsResponse getUserAlarmSettings(Long userId) {
        User user = getUserOrThrow(userId);
        return UserAlarmSettingsResponse.from(user);
    }

    @Transactional
    @CacheEvict(value = "userInternal", key = "#userId", cacheManager = "redisCacheManager")
    public UserUpdateResponse updateUserProfile(Long userId, UserProfileUpdateRequest request, UploadFile uploadFile) {
        User user = getUserOrThrow(userId);

        String profileImageUrl = null;
        if (uploadFile != null) {
            profileImageUrl = s3ImageUploader.upload(
                    uploadFile.originalFilename(),
                    uploadFile.contentType(),
                    uploadFile.inputStream(),
                    uploadFile.size());
        }

        user.updateProfile(request.nickName(), request.watchStyle(), request.favoriteClubId(), profileImageUrl);
        userRepository.save(user);

        ClubSummary club = (user.getClubId() != null) ? clubService.getClubSummary(user.getClubId()) : null;
        return UserUpdateResponse.from(user, club);
    }

    @Transactional
    @CacheEvict(value = "userInternal", key = "#userId", cacheManager = "redisCacheManager")
    public UserAlarmUpdateResponse updateUserAlarm(Long userId, UserAlarmType alarmType, boolean isEnabled) {
        User user = getUserOrThrow(userId);
        user.updateAlarm(alarmType, isEnabled);
        userRepository.save(user);
        return UserAlarmUpdateResponse.of(alarmType, isEnabled);
    }

    @Transactional
    @CacheEvict(value = "userInternal", key = "#command.userId()", cacheManager = "redisCacheManager")
    public void updateUserFcmToken(UserFcmTokenUpdateCommand command) {
        User user = getUserOrThrow(command.userId());
        user.updateFcmToken(command.fcmToken());
        userRepository.save(user);
    }

    // 다른 컨텍스트용
    @Cacheable(value = "userInternal", key = "#userId", cacheManager = "redisCacheManager", unless = "#result == null")
    public UserSummary getUserSummary(Long userId) {
        return UserSummary.from(getUserOrThrow(userId));
    }

    public List<UserSummary> getUserSummaries(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(UserSummary::from)
                .toList();
    }

    public Optional<UserSummary> findUserSummaryByProviderId(String providerIdWithProvider) {
        return userRepository.findByProviderId(providerIdWithProvider).map(UserSummary::from);
    }

    public List<UserSummary> getEventAlarmEnabledUserSummaries() {
        return userRepository.findAllEventAlarmEnabled().stream()
                .map(UserSummary::from)
                .toList();
    }

    public Page<UserSummary> getUserSummariesByClubId(Long clubId, Pageable pageable) {
        return userRepository.findAllByClubId(clubId, pageable).map(UserSummary::from);
    }

    public Map<Long, Long> getUserCountByClubId() {
        return userRepository.countUsersGroupedByClubId();
    }

    public Map<String, Long> getUserCountByWatchStyle() {
        return userRepository.countUsersByWatchStyle();
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getUserCountByGender(Character gender) {
        return userRepository.countByGender(gender);
    }

    @Transactional
    public CreatedUserResponse createUser(CreateUserCommand command) {
        if (userRepository.findByProviderId(command.providerIdWithProvider()).isPresent()) {
            throw new BaseException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User user = User.createUser(
                command.provider(),
                command.providerIdWithProvider(),
                command.email(),
                command.nickName(),
                command.gender(),
                command.birthDate(),
                command.favoriteClubId(),
                command.profileImageUrl(),
                null,
                command.watchStyle());
        User savedUser = userRepository.save(user);
        return CreatedUserResponse.from(savedUser);
    }

    @Transactional
    public void markUserAsReported(Long userId) {
        User user = getUserOrThrow(userId);
        user.markAsReported();
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "userInternal", key = "#userId", cacheManager = "redisCacheManager")
    public void clearFcmToken(Long userId) {
        User user = getUserOrThrow(userId);
        user.deleteFcmToken();
        userRepository.save(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }
}
