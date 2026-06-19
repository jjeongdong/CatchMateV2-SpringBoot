package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.ChatNotificationUseCase;
import com.back.catchmate.notification.application.port.in.NotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import com.back.catchmate.notification.application.port.in.OutboxSaveUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationChatRecipientInfo;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.ChatRoomFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserOnlineStatusFetchPort;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatNotificationService implements ChatNotificationUseCase {
    private static final String NOTIFICATION_TYPE = "CHAT";

    private final UserFetchPort userFetchPort;
    private final ChatRoomFetchPort chatRoomFetchPort;
    private final UserOnlineStatusFetchPort userOnlineStatusFetchPort;
    private final OutboxSaveUseCase outboxSaveUseCase;
    private final OutboxDispatchUseCase outboxDispatchUseCase;
    private final NotificationDispatchUseCase notificationDispatchUseCase;

    @Override
    public void saveOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content) {
        List<NotificationChatRecipientInfo> recipientsInfo = chatRoomFetchPort.getChatRoomRecipients(chatRoomId, senderId);
        if (recipientsInfo.isEmpty()) return;

        NotificationUserInfo sender = userFetchPort.getUser(senderId);
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(sender.nickName());
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);
        Map<String, String> payload = createNotificationData(chatRoomId, senderId, sender.nickName(), content, title, body);

        Map<Long, NotificationChatRecipientInfo> infoMap = recipientsInfo.stream()
                .collect(Collectors.toMap(NotificationChatRecipientInfo::userId, Function.identity()));

        List<NotificationUserInfo> recipients = userFetchPort.getUsers(recipientsInfo.stream().map(NotificationChatRecipientInfo::userId).toList());
        for (NotificationUserInfo recipient : recipients) {
            // 해당 채팅방 알림이 꺼져있으면 아웃박스 저장 안함
            if (!infoMap.get(recipient.userId()).isNotificationOn()) continue;
            // 글로벌 채팅 알림이 꺼져있거나 토큰이 없으면 저장 안함
            if (!recipient.chatAlarmEnabled() || recipient.fcmToken() == null) continue;

            // 현재 보고 있는 방이면 아웃박스 저장 안함 (FCM 발송 원천 방지)
            Long focusRoomId = userOnlineStatusFetchPort.getUserFocusRoom(recipient.userId());
            if (chatRoomId.equals(focusRoomId)) continue;

            outboxSaveUseCase.saveOutbox(
                    recipient.userId(),
                    recipient.fcmToken(),
                    title,
                    body,
                    payload
            );
        }
    }

    @Override
    public void dispatchOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content) {
        List<NotificationChatRecipientInfo> recipientsInfo = chatRoomFetchPort.getChatRoomRecipients(chatRoomId, senderId);
        if (recipientsInfo.isEmpty()) return;

        NotificationUserInfo sender = userFetchPort.getUser(senderId);
        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(sender.nickName());
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);
        Map<String, String> payload = createNotificationData(chatRoomId, senderId, sender.nickName(), content, title, body);

        Map<Long, NotificationChatRecipientInfo> infoMap = recipientsInfo.stream()
                .collect(Collectors.toMap(NotificationChatRecipientInfo::userId, Function.identity()));

        List<NotificationUserInfo> recipients = userFetchPort.getUsers(recipientsInfo.stream().map(NotificationChatRecipientInfo::userId).toList());
        for (NotificationUserInfo recipient : recipients) {
            // 현재 보고 있는 방이면 실시간 알림 스킵
            Long focusRoomId = userOnlineStatusFetchPort.getUserFocusRoom(recipient.userId());
            if (chatRoomId.equals(focusRoomId)) continue;

            // 알림 설정 여부와 상관없이 STOMP 메시지는 항상 전송 (목록 업데이트 등 UI 동기화용)
            notificationDispatchUseCase.dispatch(recipient.userId(), payload);

            // 알림이 켜져있으면 즉시 발송 시도 (Outbox Dispatch)
            if (infoMap.get(recipient.userId()).isNotificationOn() && recipient.chatAlarmEnabled()) {
                outboxDispatchUseCase.sendPendingOutboxImmediately(recipient.userId());
            }
        }
    }

    private static Map<String, String> createNotificationData(
            Long chatRoomId, Long senderId, String senderNickname, String content, String title, String body
    ) {
        return Map.of(
                "type", NOTIFICATION_TYPE,
                "roomId", chatRoomId.toString(),
                "senderId", senderId.toString(),
                "senderNickname", senderNickname,
                "content", content,
                "title", title,
                "body", body
        );
    }
}
