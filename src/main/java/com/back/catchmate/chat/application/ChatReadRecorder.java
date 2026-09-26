package com.back.catchmate.chat.application;

import com.back.catchmate.chat.domain.ChatSequenceStore;
import com.back.catchmate.chat.domain.ReadSequenceBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 읽음 처리는 Redis 버퍼에만 쓰고 스케줄러가 모아 DB 에 반영한다 (write-behind).
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReadRecorder {
    private final ChatSequenceStore chatSequenceStore;
    private final ReadSequenceBuffer readSequenceBuffer;

    // 읽음 실패로 화면 진입·구독이 막히면 안 되므로 삼키고 로그만 남긴다.
    public void record(Long chatRoomId, Long userId) {
        try {
            readSequenceBuffer.buffer(chatRoomId, userId, chatSequenceStore.current(chatRoomId));
        } catch (Exception e) {
            log.error("읽음 처리 버퍼링 실패 chatRoomId={}, userId={}", chatRoomId, userId, e);
        }
    }
}
