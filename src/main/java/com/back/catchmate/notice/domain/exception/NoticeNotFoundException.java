package com.back.catchmate.notice.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.notice.domain.NoticeErrorCode;

public class NoticeNotFoundException extends BusinessException {
    public NoticeNotFoundException() {
        super(NoticeErrorCode.NOTICE_NOT_FOUND);
    }
}
