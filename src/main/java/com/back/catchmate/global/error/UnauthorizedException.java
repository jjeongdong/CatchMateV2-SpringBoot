package com.back.catchmate.global.error;

// 특정 BC 에 속하지 않는 인증 실패(요청의 로그인 정보 누락·형식 오류, 웹소켓 연결 인증 실패)용.
public class UnauthorizedException extends BusinessException {
    public UnauthorizedException() {
        super(GlobalErrorCode.UNAUTHORIZED);
    }
}
