package com.back.catchmate.notification.domain.event;

import java.util.List;

/**
 * 채팅 메시지 알림의 발송 대상을 저장 단계에서 확정했다는 notification 내부 이벤트. 커밋 후 발송 단계가 수신자·설정·포커스를
 * 다시 조회하지 않고 이 결과로 보내게 하려고 쓴다.
 *
 * @param realtimeTargetIds 그 방을 보고 있지 않은 수신자 전원 (실시간 알림 대상)
 * @param pushRecipientIds 그중 방 알림·채팅 알림을 켠 수신자 (즉시 푸시 대상)
 */
public record ChatNotificationPreparedEvent(
        Long chatRoomId,
        Long senderId,
        String senderNickname,
        String content,
        List<Long> realtimeTargetIds,
        List<Long> pushRecipientIds) {}
