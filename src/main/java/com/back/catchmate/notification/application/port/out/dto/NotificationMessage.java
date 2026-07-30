package com.back.catchmate.notification.application.port.out.dto;

import java.util.Map;

/**
 * 배치 발송 1건. 아웃박스 행에서 전송에 필요한 값만 뽑아 전달한다.
 */
public record NotificationMessage(Long userId, String token, String title, String body, Map<String, String> data) {
}
