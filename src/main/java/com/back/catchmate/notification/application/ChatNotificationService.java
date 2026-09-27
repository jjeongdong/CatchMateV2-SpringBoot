package com.back.catchmate.notification.application;

import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.chat.application.dto.api.ChatRecipientInfo;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.NotificationTemplate;
import com.back.catchmate.notification.domain.OutboxRecipient;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 채팅은 인앱 알림함에 남기지 않는다 (옛 동작). 푸시 아웃박스와 실시간 알림만 다룬다.
@Service
@RequiredArgsConstructor
public class ChatNotificationService {
    private final UserQueryApi userQueryApi;
    private final ChatQueryApi chatQueryApi;
    private final OutboxWriter outboxWriter;
    private final OutboxDispatcher outboxDispatcher;
    private final RealtimeNotificationPublisher realtimeNotificationPublisher;

    @Transactional
    public void saveOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content) {
        List<ChatRecipientInfo> recipientInfos = chatQueryApi.getRecipients(chatRoomId, senderId);
        if (recipientInfos.isEmpty()) {
            return;
        }
        String senderNickname = userQueryApi.getInfo(senderId).nickName();
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(senderNickname);
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);
        Map<Long, ChatRecipientInfo> recipientInfosByUserId = byUserId(recipientInfos);

        // 알림 설정으로 먼저 걸러 Redis 조회 대상 자체를 줄인다.
        List<UserInfo> candidates = recipients(recipientInfos).stream()
                .filter(recipient ->
                        recipientInfosByUserId.get(recipient.userId()).isNotificationOn())
                .filter(recipient -> recipient.chatAlarmEnabled() && recipient.fcmToken() != null)
                .toList();
        // 수신자별 왕복 대신 MGET 한 번으로 포커스 방을 모아온다.
        Map<Long, Long> focusRoomsByUserId = chatQueryApi.getFocusRooms(
                candidates.stream().map(UserInfo::userId).toList());

        // 지금 보고 있는 방이면 푸시가 불필요하므로 아웃박스에 쌓지 않는다.
        List<OutboxRecipient> outboxRecipients = candidates.stream()
                .filter(recipient -> !chatRoomId.equals(focusRoomsByUserId.get(recipient.userId())))
                .map(recipient -> new OutboxRecipient(recipient.userId(), recipient.fcmToken()))
                .toList();
        outboxWriter.writeAll(
                outboxRecipients,
                title,
                body,
                NotificationPayload.chat(chatRoomId, senderId, senderNickname, content, title, body));
    }

    public void dispatchOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content) {
        List<ChatRecipientInfo> recipientInfos = chatQueryApi.getRecipients(chatRoomId, senderId);
        if (recipientInfos.isEmpty()) {
            return;
        }
        String senderNickname = userQueryApi.getInfo(senderId).nickName();
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(senderNickname);
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);
        Map<Long, ChatRecipientInfo> recipientInfosByUserId = byUserId(recipientInfos);
        List<UserInfo> recipients = recipients(recipientInfos);

        // 알림 설정과 무관하게 전원의 포커스 여부를 봐야 하므로 수신자 전체를 MGET 한 번으로 모아온다.
        Map<Long, Long> focusRoomsByUserId = chatQueryApi.getFocusRooms(
                recipients.stream().map(UserInfo::userId).toList());
        List<UserInfo> targets = recipients.stream()
                .filter(recipient -> !chatRoomId.equals(focusRoomsByUserId.get(recipient.userId())))
                .toList();

        // 알림 설정과 상관없이 실시간 알림은 보낸다 (채팅방 목록 갱신 등 화면 동기화용).
        realtimeNotificationPublisher.publishAll(
                targets.stream().map(UserInfo::userId).toList(),
                NotificationPayload.chat(chatRoomId, senderId, senderNickname, content, title, body));

        // 수신자별 단건 발송은 FCM 호출·트랜잭션이 방 인원만큼 순차로 쌓여 발송 스레드를 오래 잡는다.
        List<Long> pushRecipientIds = targets.stream()
                .filter(recipient ->
                        recipientInfosByUserId.get(recipient.userId()).isNotificationOn()
                                && recipient.chatAlarmEnabled())
                .map(UserInfo::userId)
                .toList();
        outboxDispatcher.sendPendingOutboxesImmediately(pushRecipientIds);
    }

    private List<UserInfo> recipients(List<ChatRecipientInfo> recipientInfos) {
        return List.copyOf(userQueryApi
                .getInfos(recipientInfos.stream().map(ChatRecipientInfo::userId).toList())
                .values());
    }

    private static Map<Long, ChatRecipientInfo> byUserId(List<ChatRecipientInfo> recipientInfos) {
        Map<Long, ChatRecipientInfo> recipientInfosByUserId = new HashMap<>();
        for (ChatRecipientInfo recipientInfo : recipientInfos) {
            recipientInfosByUserId.put(recipientInfo.userId(), recipientInfo);
        }
        return recipientInfosByUserId;
    }
}
