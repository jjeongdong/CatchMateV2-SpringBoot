package com.back.catchmate.user.application.service;

import com.back.catchmate.common.upload.UploadFile;
import com.back.catchmate.user.application.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.user.application.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.user.application.dto.response.UserUpdateResponse;
import com.back.catchmate.user.application.port.in.UserClientCommandUseCase;
import com.back.catchmate.user.application.port.out.dto.UserClubInfo;
import com.back.catchmate.user.application.port.out.external.ClubFetchPort;
import com.back.catchmate.user.application.port.out.external.ImageUploaderPort;
import com.back.catchmate.user.application.port.out.persistence.UserRepository;
import com.back.catchmate.user.domain.model.UserAlarmType;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserClientCommandService implements UserClientCommandUseCase {
    private final UserRepository userRepository;
    private final UserReader userReader;
    private final ClubFetchPort clubFetchPort;
    private final ImageUploaderPort imageUploaderPort;

    @Override
    @CacheEvict(value = "userInternal", key = "#userId", cacheManager = "redisCacheManager")
    public UserUpdateResponse updateUserProfile(Long userId, UserProfileUpdateCommand command, UploadFile uploadFile) {
        User user = userReader.getUser(userId);

        String profileImageUrl = null;
        if (uploadFile != null) {
            profileImageUrl = imageUploaderPort.upload(
                    uploadFile.originalFilename(),
                    uploadFile.contentType(),
                    uploadFile.inputStream(),
                    uploadFile.size()
            );
        }

        user.updateProfile(command.nickName(), command.watchStyle(), command.favoriteClubId(), profileImageUrl);
        userRepository.save(user);

        UserClubInfo club = (user.getClubId() != null) ? clubFetchPort.getClub(user.getClubId()) : null;
        return UserUpdateResponse.from(user, club);
    }

    @Override
    @CacheEvict(value = "userInternal", key = "#userId", cacheManager = "redisCacheManager")
    public UserAlarmUpdateResponse updateUserAlarm(Long userId, UserAlarmType alarmType, boolean isEnabled) {
        User user = userReader.getUser(userId);
        user.updateAlarm(alarmType, isEnabled);
        userRepository.save(user);
        return UserAlarmUpdateResponse.of(alarmType, isEnabled);
    }

    @Override
    @CacheEvict(value = "userInternal", key = "#command.userId()", cacheManager = "redisCacheManager")
    public void updateUserFcmToken(UserFcmTokenUpdateCommand command) {
        User user = userReader.getUser(command.userId());
        user.updateFcmToken(command.fcmToken());
        userRepository.save(user);
    }
}
