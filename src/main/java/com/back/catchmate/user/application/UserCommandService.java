package com.back.catchmate.user.application;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.user.application.dto.command.UserAlarmUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserCreateCommand;
import com.back.catchmate.user.application.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.user.application.dto.result.UserAlarmResult;
import com.back.catchmate.user.application.dto.result.UserCreateResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.domain.ProfileImageUploader;
import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserPresenceRepository;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.domain.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandService {
    private final UserRepository userRepository;
    private final UserPresenceRepository userPresenceRepository;
    private final ProfileImageUploader profileImageUploader;
    private final ClubQueryApi clubQueryApi;

    @Transactional
    public UserResult updateMyProfile(Long userId, UserProfileUpdateCommand command, UploadFile profileImage) {
        User user = userRepository.getById(userId);
        String profileImageUrl = profileImage != null ? profileImageUploader.upload(profileImage) : null;
        user.updateProfile(command.nickName(), command.watchStyle(), command.clubId(), profileImageUrl);
        return UserResult.of(user, clubQueryApi.getInfo(user.getClubId()));
    }

    @Transactional
    public UserAlarmResult updateMyAlarm(Long userId, UserAlarmUpdateCommand command) {
        User user = userRepository.getById(userId);
        user.updateAlarm(command.alarmType(), command.enabled());
        return UserAlarmResult.from(user);
    }

    @Transactional
    public void updateFcmToken(UserFcmTokenUpdateCommand command) {
        userRepository.getById(command.userId()).updateFcmToken(command.fcmToken());
    }

    /**
     * 미전환 oauth 가입 흐름 전용 동기 호출. userId 를 곧바로 받아 토큰을 발급해야 해서 이벤트로 바꿀 수 없다.
     * oauth/auth 전환 때 가입 유스케이스의 소유권과 함께 다시 정한다.
     */
    @Transactional
    public UserCreateResult createUser(UserCreateCommand command) {
        // users.provider_id 에 유니크 제약이 없고 운영 스키마를 바꾸지 않기로 해서 조회로 중복 가입을 막는다.
        if (userRepository.findByProviderId(command.providerIdWithProvider()).isPresent()) {
            throw new UserAlreadyExistsException();
        }
        User user = userRepository.save(User.create(
                command.provider(),
                command.providerIdWithProvider(),
                command.email(),
                command.nickName(),
                command.gender(),
                command.birthDate(),
                command.favoriteClubId(),
                command.profileImageUrl(),
                command.watchStyle()));
        return UserCreateResult.from(user);
    }

    /** 미전환 auth 로그아웃 전용 동기 호출. auth 전환 시 로그아웃 이벤트로 대체한다. */
    @Transactional
    public void clearFcmToken(Long userId) {
        userRepository.getById(userId).clearFcmToken();
    }

    /** 신고 처리 이벤트(UserReportProcessedListener) 전용. */
    @Transactional
    public void markUserAsReported(Long userId) {
        userRepository.getById(userId).markAsReported();
    }

    /** 미전환 chat 의 WebSocket 연결 이벤트 전용. Redis 전용이라 트랜잭션을 열지 않는다. chat 전환 시 재검토. */
    public void markOnline(Long userId) {
        userPresenceRepository.markOnline(userId);
    }

    /** 미전환 chat 의 WebSocket 해제 이벤트 전용. Redis 전용이라 트랜잭션을 열지 않는다. chat 전환 시 재검토. */
    public void markOffline(Long userId) {
        userPresenceRepository.markOffline(userId);
    }

    /** 미전환 chat 의 채팅방 구독 이벤트 전용. Redis 전용이라 트랜잭션을 열지 않는다. chat 전환 시 재검토. */
    public void focusRoom(Long userId, Long roomId) {
        userPresenceRepository.focusRoom(userId, roomId);
    }

    /** 미전환 chat 의 채팅방 구독 해제 이벤트 전용. Redis 전용이라 트랜잭션을 열지 않는다. chat 전환 시 재검토. */
    public void unfocusRoom(Long userId) {
        userPresenceRepository.unfocusRoom(userId);
    }
}
