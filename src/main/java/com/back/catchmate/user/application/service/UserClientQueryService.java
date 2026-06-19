package com.back.catchmate.user.application.service;

import com.back.catchmate.user.application.dto.response.UserAlarmSettingsResponse;
import com.back.catchmate.user.application.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import com.back.catchmate.user.application.port.in.UserClientQueryUseCase;
import com.back.catchmate.user.application.port.out.dto.UserClubInfo;
import com.back.catchmate.user.application.port.out.external.ClubFetchPort;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserClientQueryService implements UserClientQueryUseCase {
    private final UserReader userReader;
    private final ClubFetchPort clubFetchPort;

    @Override
    public UserResponse getUserProfile(Long userId) {
        User user = userReader.getUser(userId);
        UserClubInfo club = user.getClubId() != null ? clubFetchPort.getClub(user.getClubId()) : null;
        return UserResponse.from(user, club);
    }

    @Override
    public UserResponse getUserProfileById(Long currentUserId, Long targetUserId) {
        User targetUser = userReader.getUser(targetUserId);
        UserClubInfo club = targetUser.getClubId() != null ? clubFetchPort.getClub(targetUser.getClubId()) : null;
        return UserResponse.from(targetUser, club);
    }

    @Override
    public UserNicknameCheckResponse getUserNicknameAvailability(String nickName) {
        boolean isAvailable = !userReader.existsByNickName(nickName);
        return UserNicknameCheckResponse.of(nickName, isAvailable);
    }

    @Override
    public UserAlarmSettingsResponse getUserAlarmSettings(Long userId) {
        User user = userReader.getUser(userId);
        return UserAlarmSettingsResponse.from(user);
    }
}
