package com.back.catchmate.application.chat.port;

import com.back.catchmate.application.chat.event.ChatMessageEvent;

import java.util.Map;

public interface MessagePublisher {
    /**
     * 메시지 브로커(Redis)에 메시지를 발행합니다.
     * @param topicName 발행할 토픽 이름 (예: chat-room)
     * @param message 전송할 메시지 객체
     */
    void publish(String topicName, ChatMessageEvent message);

    void publishNotification(Long userId, Map<String, String> data);
}
