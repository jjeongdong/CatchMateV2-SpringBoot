package com.back.catchmate.enroll.application.port.out.external;

public interface IdempotencyPort {
    /**
     * 키가 없으면 set 후 true 반환, 이미 있으면 false 반환.
     */
    boolean acquireIfAbsent(String key, long ttlSeconds);

    /**
     * 처리가 끝난 키를 즉시 해제한다. TTL 만료 전에도 "처리 중"과 "완료됨"을 구분하기 위함.
     */
    void release(String key);
}
