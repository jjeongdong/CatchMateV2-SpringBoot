package com.back.catchmate.chat.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// 사용자가 지금 보고 있는 채팅방. 그 방의 새 메시지 푸시를 억제하는 데 쓴다.
public interface ChatFocusRoomStore {

    void focus(Long userId, Long chatRoomId);

    void unfocus(Long userId);

    Optional<Long> find(Long userId);

    // 포커스 중인 사용자만 담긴다.
    Map<Long, Long> findAll(List<Long> userIds);
}
