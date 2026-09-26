package com.back.catchmate.user.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.user.domain.UserErrorCode;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}
