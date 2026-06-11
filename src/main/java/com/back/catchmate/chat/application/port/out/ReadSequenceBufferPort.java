package com.back.catchmate.chat.application.port.out;

import java.util.Map;

public interface ReadSequenceBufferPort {
    void buffer(Long chatRoomId, Long userId, Long sequence);

    Map<String, Long> drainAll();
}
