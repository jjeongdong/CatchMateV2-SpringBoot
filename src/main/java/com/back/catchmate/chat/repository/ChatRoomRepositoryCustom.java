package com.back.catchmate.chat.repository;

import java.util.Map;

public interface ChatRoomRepositoryCustom {
    /**
     * 버퍼에서 드레인한 채팅방 시퀀스를 JDBC batch 로 일괄 반영한다.
     * 역전 방지를 위해 기존 값보다 큰 경우에만 갱신한다.
     */
    void updateMaxSequencesBatch(Map<Long, Long> sequences);
}
