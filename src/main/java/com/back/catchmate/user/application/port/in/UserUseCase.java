package com.back.catchmate.user.application.port.in;

import com.back.catchmate.user.application.dto.command.UploadFile;
import com.back.catchmate.user.application.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserRegisterCommand;
import com.back.catchmate.user.application.dto.response.UserAlarmSettingsResponse;
import com.back.catchmate.user.application.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.user.application.dto.response.UserNicknameCheckResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import com.back.catchmate.user.application.dto.response.UserSignupResult;
import com.back.catchmate.user.application.dto.response.UserUpdateResponse;
import com.back.catchmate.user.domain.enums.AlarmType;

public interface UserUseCase {
    UserSignupResult createUser(UserRegisterCommand command);
    UserResponse getUserProfile(Long userId);
    UserResponse getUserProfileById(Long currentUserId, Long targetUserId);
    UserNicknameCheckResponse getUserNicknameAvailability(String nickName);
    UserUpdateResponse updateUserProfile(Long userId, UserProfileUpdateCommand command, UploadFile uploadFile);
    UserAlarmUpdateResponse updateUserAlarm(Long userId, AlarmType alarmType, boolean isEnabled);
    UserAlarmSettingsResponse getUserAlarmSettings(Long userId);
    void updateUserFcmToken(Long userId, UserFcmTokenUpdateCommand command);
}
