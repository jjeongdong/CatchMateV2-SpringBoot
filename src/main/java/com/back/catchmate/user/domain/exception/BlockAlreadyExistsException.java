package com.back.catchmate.user.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.user.domain.UserErrorCode;

public class BlockAlreadyExistsException extends BusinessException {
    public BlockAlreadyExistsException() {
        super(UserErrorCode.BLOCK_ALREADY_EXISTS);
    }
}
