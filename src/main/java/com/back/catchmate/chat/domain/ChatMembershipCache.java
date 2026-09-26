package com.back.catchmate.chat.domain;

import java.util.Optional;

// 멤버십 인증 캐시. 멤버십이 바뀌는 모든 경로에서 evict 해야 한다.
public interface ChatMembershipCache {

    Optional<MembershipSnapshot> find(Long chatRoomId, Long userId);

    void put(Long chatRoomId, Long userId, MembershipSnapshot snapshot);

    void evict(Long chatRoomId, Long userId);
}
