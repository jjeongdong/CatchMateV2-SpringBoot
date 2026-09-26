package com.back.catchmate.chat.domain;

// 메시지 기록 캐시. 새 메시지가 생기면 최신 페이지(커서 없는 첫 페이지)만 지운다.
public interface ChatHistoryCache {

    void evictLatestPage(Long chatRoomId);
}
