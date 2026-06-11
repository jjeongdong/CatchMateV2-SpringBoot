package com.back.catchmate.domain.chat.port;

import java.util.Map;

public interface ReadSequenceBufferPort {
    void buffer(Long chatRoomId, Long userId, Long sequence);

    Map<String, Long> drainAll();
}
