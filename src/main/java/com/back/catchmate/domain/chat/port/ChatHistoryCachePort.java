package com.back.catchmate.domain.chat.port;

public interface ChatHistoryCachePort {
    void evictLatestPage(Long chatRoomId);
}
