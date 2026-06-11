package com.back.catchmate.domain.common.idempotency;

public interface IdempotencyPort {
    /**
     * 키가 없으면 set 후 true 반환, 이미 있으면 false 반환.
     */
    boolean acquireIfAbsent(String key, long ttlSeconds);
}
