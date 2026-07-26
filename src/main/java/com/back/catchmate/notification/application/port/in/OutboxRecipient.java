package com.back.catchmate.notification.application.port.in;

// 배치 Outbox 저장 시 수신자별로 달라지는 값만 담는다(title/body/payload 는 공유).
public record OutboxRecipient(Long recipientId, String recipientAddress) {
}
