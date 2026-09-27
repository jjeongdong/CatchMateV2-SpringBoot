package com.back.catchmate.notification.domain;

import java.util.Map;

// 푸시 1건. 아웃박스 행에서 발송에 필요한 값만 뽑는다.
public record PushMessage(Long userId, String token, String title, String body, Map<String, String> data) {}
