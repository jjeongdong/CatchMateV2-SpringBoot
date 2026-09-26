package com.back.catchmate.common.error.exception;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.global.error.BusinessException;

// 마이그레이션 과도기용. 전환 전 BC 의 new BaseException(ErrorCode.XXX) 를 새 핸들러 경로로 흘려보낸다.
public class BaseException extends BusinessException {
    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    // 기존 호출부가 옛 enum 타입을 그대로 쓰도록 좁힌 타입으로 돌려준다.
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
