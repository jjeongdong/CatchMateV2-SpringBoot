package com.back.catchmate.chat.application.port.out;

import com.back.catchmate.chat.application.port.out.persistence.ReadSequenceUpdate;
import java.util.List;

public interface ReadSequenceBufferPort {
    void buffer(Long chatRoomId, Long userId, Long sequence);

    List<ReadSequenceUpdate> drainAll();
}
