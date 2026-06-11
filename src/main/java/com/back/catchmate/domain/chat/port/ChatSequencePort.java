package com.back.catchmate.domain.chat.port;

public interface ChatSequencePort {
    /**
     * 채팅방별 시퀀스 번호 생성
     */
    Long generateSequence(Long roomId);

    /**
     * 채팅방의 현재 시퀀스 번호 조회
     */
    Long getCurrentSequence(Long roomId);
}
