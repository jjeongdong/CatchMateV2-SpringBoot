package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.back.catchmate.notification.application.port.out.OfflineFallbackPort;
import com.back.catchmate.notification.application.port.out.NotificationSenderPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeNotificationDispatcher implements OfflineFallbackPort {

    private final ObjectMapper objectMapper;

    private final List<NotificationSenderPort> senders;

    @Override
    public void dispatchIfOffline(Long userId, NotificationOutbox outbox) {
        senders.stream()
                .filter(sender -> sender.supports(outbox.getChannel()))
                .findFirst()
                .ifPresentOrElse(
                        sender -> sender.sendNotificationIfOffline(
                                userId,
                                outbox.getRecipientAddress(),
                                outbox.getTitle(),
                                outbox.getBody(),
                                parsePayload(outbox.getPayload())
                        ),
                        () -> log.warn("지원하는 채널 구현체 없음: channel={}", outbox.getChannel())
                );
    }

    private Map<String, String> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("페이로드 파싱 실패 - 빈 Map으로 대체: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
