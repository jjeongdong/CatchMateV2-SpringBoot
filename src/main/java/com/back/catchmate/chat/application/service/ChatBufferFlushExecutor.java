package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.port.out.persistence.ChatRoomMemberRepository;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomRepository;
import com.back.catchmate.chat.application.port.out.persistence.ReadSequenceUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatBufferFlushExecutor {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public void flushReadSequences(List<ReadSequenceUpdate> updates) {
        chatRoomMemberRepository.updateLastReadSequencesBatch(updates);
        log.debug("읽음 시퀀스 {} 건 DB 반영 완료", updates.size());
    }

    @Transactional
    public void flushRoomSequences(Map<Long, Long> sequences) {
        for (Map.Entry<Long, Long> entry : sequences.entrySet()) {
            chatRoomRepository.updateMaxSequence(entry.getKey(), entry.getValue());
        }

        log.debug("채팅방 시퀀스 {} 건 DB 반영 완료", sequences.size());
    }
}
