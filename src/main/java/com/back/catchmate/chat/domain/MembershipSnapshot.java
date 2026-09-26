package com.back.catchmate.chat.domain;

import com.back.catchmate.chat.domain.exception.ChatRoomMemberNotFoundException;
import com.back.catchmate.chat.domain.exception.ChatRoomReadOnlyException;

/**
 * 채팅방 멤버십(활성/읽기전용) 인증 캐시의 스냅샷.
 * send·SUBSCRIBE 마다 반복되던 DB 멤버십 조회를 Redis 로 흡수해 DB 커넥션 풀 압박을 줄인다.
 * 멤버십 변경 시 반드시 ChatMembershipCache.evict 를 부른다.
 */
public record MembershipSnapshot(boolean active, boolean readOnly) {

    public void verifyActive() {
        if (!active) {
            throw new ChatRoomMemberNotFoundException();
        }
    }

    public void verifySendable() {
        verifyActive();
        if (readOnly) {
            throw new ChatRoomReadOnlyException();
        }
    }
}
