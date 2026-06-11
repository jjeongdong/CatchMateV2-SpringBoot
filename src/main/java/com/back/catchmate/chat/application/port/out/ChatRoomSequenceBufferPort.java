package com.back.catchmate.chat.application.port.out;

import java.util.Map;

public interface ChatRoomSequenceBufferPort {
    void buffer(Long chatRoomId, Long sequence);

    Map<Long, Long> drainAll();
}
