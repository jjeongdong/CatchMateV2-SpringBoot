package com.back.catchmate.chat.domain;

import java.util.List;

// 읽음 시퀀스 write-behind 버퍼. (방, 사용자)별로 가장 큰 값만 남는다(monotonic).
public interface ReadSequenceBuffer {

    void buffer(Long chatRoomId, Long userId, Long sequence);

    // 꺼내면서 비운다.
    List<ReadSequence> drainAll();
}
