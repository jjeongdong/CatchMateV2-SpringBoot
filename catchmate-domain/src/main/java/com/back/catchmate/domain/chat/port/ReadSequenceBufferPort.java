package com.back.catchmate.domain.chat.port;

import java.util.List;
import java.util.Map;

public interface ReadSequenceBufferPort {
    void buffer(Long chatRoomId, Long userId, Long sequence);

    Map<Long, Long> getBufferedSequences(List<Long> chatRoomIds, Long userId);

    Map<String, Long> drainAll();

    int size();
}
