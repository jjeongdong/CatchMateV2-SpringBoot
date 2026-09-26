package com.back.catchmate.chat.application;

import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.domain.ReadSequence;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 드레인·복원은 트랜잭션 밖에서, DB 반영만 좁은 트랜잭션으로 하려고 별도 빈으로 둔다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBufferFlushExecutor {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public void flushReadSequences(List<ReadSequence> readSequences) {
        chatRoomMemberRepository.updateLastReadSequences(readSequences);
        log.debug("읽음 시퀀스 DB 반영 count={}", readSequences.size());
    }

    @Transactional
    public void flushRoomSequences(Map<Long, Long> sequenceByChatRoomId) {
        chatRoomRepository.updateMaxSequences(sequenceByChatRoomId);
        log.debug("채팅방 시퀀스 DB 반영 count={}", sequenceByChatRoomId.size());
    }
}
