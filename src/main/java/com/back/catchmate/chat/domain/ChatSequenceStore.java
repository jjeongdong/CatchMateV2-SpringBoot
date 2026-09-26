package com.back.catchmate.chat.domain;

// 방별 메시지 순번. INCR 이 원자적이라 여러 서버가 동시에 보내도 순번이 겹치지 않는다.
public interface ChatSequenceStore {

    Long next(Long chatRoomId);

    // 발급된 적이 없으면 0
    Long current(Long chatRoomId);
}
