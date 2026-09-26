package com.back.catchmate.notification.service;

import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.chat.application.dto.api.ChatRecipientInfo;
import com.back.catchmate.notification.entity.enums.NotificationTemplate;
import com.back.catchmate.notification.infra.RedisNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 채팅 알림의 비동기 발송 전용 서비스(비트랜잭션).
 * FCM 호출 동안 DB 커넥션을 점유하지 않기 위해 {@link ChatNotificationService}(저장) 와 분리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotificationDispatchService {
    private static final String NOTIFICATION_TYPE = "CHAT";

    private final UserQueryApi userQueryApi;
    private final ChatQueryApi chatQueryApi;
    private final OutboxDispatcher outboxDispatcher;
    private final RedisNotificationPublisher redisNotificationPublisher;

    /**
     * 채팅 메시지 전송 시, 수신자별 알림 설정과 포커스 여부를 확인하여 알림을 발송한다.
     * <p>
     * 1. 수신자 목록 조회 (보낸 사람 제외)
     * 2. 수신자별 알림 설정 조회
     * 3. 수신자별 포커스 여부 조회
     * 4. STOMP 메시지 전송 (포커스 여부와 상관없이 전송)
     * 5. 알림 설정이 켜져있고, 포커스가 없는 경우 Outbox Dispatch 즉시 발송
     */
    public void dispatchOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content) {
        List<ChatRecipientInfo> recipientsInfo = chatQueryApi.getRecipients(chatRoomId, senderId);
        if (recipientsInfo.isEmpty()) return;

        UserInfo sender = userQueryApi.getInfo(senderId);
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(sender.nickName());
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);
        Map<String, String> payload =
                createNotificationData(chatRoomId, senderId, sender.nickName(), content, title, body);

        Map<Long, ChatRecipientInfo> infoMap =
                recipientsInfo.stream().collect(Collectors.toMap(ChatRecipientInfo::userId, Function.identity()));

        List<UserInfo> recipients = List.copyOf(userQueryApi
                .getInfos(recipientsInfo.stream().map(ChatRecipientInfo::userId).toList())
                .values());

        // 알림 설정과 무관하게 전원의 포커스 여부를 봐야 하므로 수신자 전체를 MGET 한 번으로 모아온다.
        Map<Long, Long> focusRooms = chatQueryApi.getFocusRooms(
                recipients.stream().map(UserInfo::userId).toList());

        // 현재 보고 있는 방이면 실시간 알림 스킵
        List<UserInfo> targets = recipients.stream()
                .filter(recipient -> !chatRoomId.equals(focusRooms.get(recipient.userId())))
                .toList();

        // 알림 설정 여부와 상관없이 STOMP 메시지는 항상 전송 (목록 업데이트 등 UI 동기화용).
        // 방 인원 전원이 같은 payload 를 받으므로 수신자별 publish 대신 한 건으로 묶는다.
        redisNotificationPublisher.dispatchAll(
                targets.stream().map(UserInfo::userId).toList(), payload);

        // 알림이 켜져있으면 즉시 발송 시도 (Outbox Dispatch)
        for (UserInfo recipient : targets) {
            if (infoMap.get(recipient.userId()).isNotificationOn() && recipient.chatAlarmEnabled()) {
                outboxDispatcher.sendPendingOutboxImmediately(recipient.userId());
            }
        }
    }

    private static Map<String, String> createNotificationData(
            Long chatRoomId, Long senderId, String senderNickname, String content, String title, String body) {
        return Map.of(
                "type", NOTIFICATION_TYPE,
                "roomId", chatRoomId.toString(),
                "senderId", senderId.toString(),
                "senderNickname", senderNickname,
                "content", content,
                "title", title,
                "body", body);
    }
}
