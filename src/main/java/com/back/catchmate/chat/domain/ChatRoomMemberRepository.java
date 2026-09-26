package com.back.catchmate.chat.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository {

    ChatRoomMember save(ChatRoomMember member);

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    List<ChatRoomMember> findActiveByChatRoomId(Long chatRoomId);

    // 방까지 함께 읽는다 (목록 조립에서 방 id 로 묶기 위해)
    List<ChatRoomMember> findActiveByChatRoomIdsAndUserId(Collection<Long> chatRoomIds, Long userId);

    // 버퍼에서 드레인한 읽음 시퀀스를 배치로 반영한다. 활성 멤버이고 기존 값보다 클 때만 갱신한다.
    void updateLastReadSequences(List<ReadSequence> readSequences);
}
