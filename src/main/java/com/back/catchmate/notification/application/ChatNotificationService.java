package com.back.catchmate.notification.application;

import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.chat.application.dto.api.ChatRecipientInfo;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.NotificationTemplate;
import com.back.catchmate.notification.domain.OutboxRecipient;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.notification.domain.event.ChatNotificationPreparedEvent;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    // 발송 대상(포커스·알림 설정)은 여기서 한 번만 정한다. 커밋 후 발송 단계가 수신자·유저·포커스를 다시 조회하면
    // 메시지마다 같은 DB·Redis 조회가 두 벌 돈다 (1,200명 벤치에서 발송 스레드가 앱 CPU 의 ~30%).
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
        List<UserInfo> recipients = recipients(recipientInfos);

        // 실시간 알림은 알림 설정과 무관하게 보내므로 수신자 전원의 포커스를 MGET 한 번으로 모아온다.
        Map<Long, Long> focusRoomsByUserId = chatQueryApi.getFocusRooms(
                recipients.stream().map(UserInfo::userId).toList());
        // 지금 보고 있는 방이면 푸시도 실시간 알림도 불필요하다.
        List<UserInfo> targets = recipients.stream()
                .filter(recipient -> !chatRoomId.equals(focusRoomsByUserId.get(recipient.userId())))
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        List<UserInfo> pushTargets = targets.stream()
                .filter(recipient ->
                        recipientInfosByUserId.get(recipient.userId()).isNotificationOn()
                                && recipient.chatAlarmEnabled())
                .toList();

        outboxWriter.writeAll(
                pushTargets.stream()
                        .filter(recipient -> recipient.fcmToken() != null)
                        .map(recipient -> new OutboxRecipient(recipient.userId(), recipient.fcmToken()))
                        .toList(),
                title,
                body,
                NotificationPayload.chat(chatRoomId, senderId, senderNickname, content, title, body));
        eventPublisher.publishEvent(new ChatNotificationPreparedEvent(
                chatRoomId,
                senderId,
                senderNickname,
                content,
                targets.stream().map(UserInfo::userId).toList(),
                pushTargets.stream().map(UserInfo::userId).toList()));
    }

    public void dispatchOnChatNotificationPrepared(ChatNotificationPreparedEvent event) {
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(event.senderNickname());
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(event.content());

        // 알림 설정과 상관없이 실시간 알림은 보낸다 (채팅방 목록 갱신 등 화면 동기화용).
        realtimeNotificationPublisher.publishAll(
                event.realtimeTargetIds(),
                NotificationPayload.chat(
                        event.chatRoomId(), event.senderId(), event.senderNickname(), event.content(), title, body));

        // 수신자별 단건 발송은 FCM 호출·트랜잭션이 방 인원만큼 순차로 쌓여 발송 스레드를 오래 잡는다.
        outboxDispatcher.sendPendingOutboxesImmediately(event.pushRecipientIds());
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
