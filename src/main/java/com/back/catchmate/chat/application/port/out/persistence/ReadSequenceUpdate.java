package com.back.catchmate.chat.application.port.out.persistence;

/**
 * 읽음 시퀀스 일괄 반영(write-behind flush)용 단일 갱신 엔트리.
 */
public record ReadSequenceUpdate(Long chatRoomId, Long userId, Long sequence) {
}
