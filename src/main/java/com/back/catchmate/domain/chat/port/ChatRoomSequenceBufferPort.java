package com.back.catchmate.domain.chat.port;

import java.util.Map;

public interface ChatRoomSequenceBufferPort {
    void buffer(Long chatRoomId, Long sequence);

    Map<Long, Long> drainAll();
}
