package com.back.catchmate.notification.domain;

import java.util.List;

// 모바일 푸시 발송 포트. 실패도 예외가 아니라 결과로 알린다 — 재시도 판단은 아웃박스가 한다.
public interface PushSender {

    PushOutcome send(PushMessage message);

    // 입력과 같은 순서·같은 크기의 결과를 돌려준다. 호출자는 인덱스로 원본과 짝짓는다.
    List<PushOutcome> sendAll(List<PushMessage> messages);
}
