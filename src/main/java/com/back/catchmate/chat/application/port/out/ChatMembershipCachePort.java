package com.back.catchmate.chat.application.port.out;

import java.util.Optional;

/**
 * 채팅방 멤버십(활성/읽기전용) 인증 캐시.
 * send·SUBSCRIBE 마다 반복되던 DB 멤버십 조회를 Redis 로 흡수해 DB 커넥션 풀 압박을 줄인다.
 * 멤버십 변경 시 반드시 evict (choke point = ChatRoomMemberRepository.save).
 */
public interface ChatMembershipCachePort {
    Optional<MembershipSnapshot> find(Long chatRoomId, Long userId);

    void put(Long chatRoomId, Long userId, MembershipSnapshot snapshot);

    void evict(Long chatRoomId, Long userId);

    record MembershipSnapshot(boolean active, boolean readOnly) {
    }
}
