package com.back.catchmate.user.application.port.in;

import com.back.catchmate.user.application.dto.response.UserAlarmSettingsResponse;
import com.back.catchmate.user.application.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;

public interface UserClientQueryUseCase {
    UserResponse getUserProfile(Long userId);

    UserResponse getUserProfileById(Long currentUserId, Long targetUserId);

    UserNicknameCheckResponse getUserNicknameAvailability(String nickName);

    UserAlarmSettingsResponse getUserAlarmSettings(Long userId);
}
