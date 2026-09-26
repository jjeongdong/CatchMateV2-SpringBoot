package com.back.catchmate.user.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.user.domain.UserErrorCode;

public class BlockNotFoundException extends BusinessException {
    public BlockNotFoundException() {
        super(UserErrorCode.BLOCK_NOT_FOUND);
    }
}
