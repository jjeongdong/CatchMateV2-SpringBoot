package com.back.catchmate.chat.repository;

import com.back.catchmate.chat.dto.ReadSequenceUpdate;

import java.util.List;

public interface ChatRoomMemberRepositoryCustom {
    /**
     * 버퍼에서 드레인한 읽음 시퀀스를 JDBC batch 로 일괄 반영한다.
     * 역전 방지를 위해 기존 값보다 큰 경우에만 갱신한다.
     */
    void updateLastReadSequencesBatch(List<ReadSequenceUpdate> updates);
}
