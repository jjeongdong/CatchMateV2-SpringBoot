package com.back.catchmate.chat.domain;

import java.util.Map;

// 방 시퀀스 write-behind 버퍼. 방별로 가장 큰 값만 남는다(monotonic).
public interface ChatRoomSequenceBuffer {

    void buffer(Long chatRoomId, Long sequence);

    // 꺼내면서 비운다.
    Map<Long, Long> drainAll();
}
