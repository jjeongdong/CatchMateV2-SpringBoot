package com.back.catchmate.chat.application.port.out;

public interface ChatHistoryCachePort {
    void evictLatestPage(Long chatRoomId);
}
