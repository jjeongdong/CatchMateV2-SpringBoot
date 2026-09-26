package com.back.catchmate.chat.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChatRoomRepository {

    ChatRoom save(ChatRoom chatRoom);

    // 없으면 ChatRoomNotFoundException
    ChatRoom getById(Long chatRoomId);

    // SELECT 없이 FK 연결용 참조만 얻는다 (메시지 저장 경로).
    ChatRoom getReference(Long chatRoomId);

    Optional<ChatRoom> findByBoardId(Long boardId);

    // 사용자가 활성 멤버인 방, 생성 시각 내림차순 → id 내림차순
    List<ChatRoom> findAllByMemberUserId(Long userId, long offset, int limit);

    long countByMemberUserId(Long userId);

    // 버퍼에서 드레인한 방 시퀀스를 배치로 반영한다. 기존 값보다 클 때만 갱신한다.
    void updateMaxSequences(Map<Long, Long> sequenceByChatRoomId);
}
