package com.back.catchmate.user.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.user.domain.UserErrorCode;

public class UserAlreadyExistsException extends BusinessException {
    public UserAlreadyExistsException() {
        super(UserErrorCode.USER_ALREADY_EXISTS);
    }
}
