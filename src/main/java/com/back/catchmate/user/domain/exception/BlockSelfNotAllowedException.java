package com.back.catchmate.user.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.user.domain.UserErrorCode;

public class BlockSelfNotAllowedException extends BusinessException {
    public BlockSelfNotAllowedException() {
        super(UserErrorCode.BLOCK_SELF_NOT_ALLOWED);
    }
}
