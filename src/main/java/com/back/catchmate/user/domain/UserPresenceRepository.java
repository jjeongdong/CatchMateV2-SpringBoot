package com.back.catchmate.user.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 온라인 여부와 현재 보고 있는 채팅방(포커스 방).
 * 구현은 장애 시 예외를 던지지 않고 "오프라인·포커스 없음" 쪽 기본값을 돌려준다 — 알림이 계속 나가게 하기 위함.
 */
public interface UserPresenceRepository {
    void markOnline(Long userId);

    void markOffline(Long userId);

    void focusRoom(Long userId, Long roomId);

    void unfocusRoom(Long userId);

    Optional<Long> findFocusRoom(Long userId);

    /** 포커스 중인 사용자만 담긴다. */
    Map<Long, Long> findFocusRooms(List<Long> userIds);
}
