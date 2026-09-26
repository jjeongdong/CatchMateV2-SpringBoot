package com.back.catchmate.chat.application.dto.api;

// 채팅 알림 수신 후보 (발신자 제외 활성 멤버). isNotificationOn 은 이 방의 알림 설정이다.
public record ChatRecipientInfo(Long userId, boolean isNotificationOn) {}
