package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.ChatNotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.NotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationChatRecipientInfo;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.ChatRoomFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserOnlineStatusFetchPort;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채팅 알림의 비동기 발송 전용 서비스(비트랜잭션).
 * FCM 호출 동안 DB 커넥션을 점유하지 않기 위해 {@link ChatNotificationService}(저장) 와 분리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotificationDispatchService implements ChatNotificationDispatchUseCase {
    private static final String NOTIFICATION_TYPE = "CHAT";

    private final UserFetchPort userFetchPort;
    private final ChatRoomFetchPort chatRoomFetchPort;
    private final UserOnlineStatusFetchPort userOnlineStatusFetchPort;
    private final OutboxDispatchUseCase outboxDispatchUseCase;
    private final NotificationDispatchUseCase notificationDispatchUseCase;

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
