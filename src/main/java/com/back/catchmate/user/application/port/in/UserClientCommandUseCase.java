package com.back.catchmate.user.application.port.in;

import com.back.catchmate.common.upload.UploadFile;
import com.back.catchmate.user.application.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.user.application.dto.response.UserAlarmUpdateResponse;
import com.back.catchmate.user.application.dto.response.UserUpdateResponse;
import com.back.catchmate.user.domain.model.UserAlarmType;

public interface UserClientCommandUseCase {
    UserUpdateResponse updateUserProfile(Long userId, UserProfileUpdateCommand command, UploadFile uploadFile);

    UserAlarmUpdateResponse updateUserAlarm(Long userId, UserAlarmType alarmType, boolean isEnabled);

    void updateUserFcmToken(UserFcmTokenUpdateCommand command);
}
