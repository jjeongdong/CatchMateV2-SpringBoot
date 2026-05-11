package com.back.catchmate.domain.chat.port;

import java.util.List;
import java.util.Map;

public interface ChatSequencePort {
    /**
     * 채팅방별 시퀀스 번호 생성
     */
    Long generateSequence(Long roomId);

    /**
     * 채팅방의 현재 시퀀스 번호 조회. Redis miss 시 chat_message MAX(sequence)로 lazy load.
     */
    Long getCurrentSequence(Long roomId);

    /**
     * 여러 채팅방의 현재 시퀀스를 한 번에 조회. Redis miss는 DB에서 일괄 fallback.
     */
    Map<Long, Long> getCurrentSequences(List<Long> roomIds);
}
