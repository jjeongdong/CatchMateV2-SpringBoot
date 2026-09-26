package com.back.catchmate.enroll.domain;

// 같은 신청의 수락 요청이 동시에 여러 번 들어오는 것을 막는 선점 저장소. 구현(Redis)은 infrastructure 에 둔다.
public interface EnrollAcceptIdempotencyStore {

    // 선점하면 true. 저장소 장애 시에는 수락을 막지 않도록 true 를 돌려준다.
    boolean acquire(Long enrollId);

    void release(Long enrollId);
}
