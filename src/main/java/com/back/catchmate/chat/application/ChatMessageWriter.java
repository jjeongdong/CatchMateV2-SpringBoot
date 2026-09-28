package com.back.catchmate.chat.application;

import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.domain.event.ChatMessageSentEvent;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 전송 경로에서 DB 커넥션을 잡는 유일한 구간이다. ChatCommandService 의 자기 호출로는 트랜잭션이 걸리지 않아 별도 빈으로 둔다.
@Service
@RequiredArgsConstructor
public class ChatMessageWriter {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserQueryApi userQueryApi;
    private final ApplicationEventPublisher eventPublisher;

    // Outbox 2단계(절대 변경 금지): INSERT 와 ChatMessageSentEvent 발행이 같은 트랜잭션이어야
    // notification 의 커밋 전 Outbox 저장이 원자적으로 커밋된다. 방송·알림 발송은 AFTER_COMMIT 이다.
    // 발신자도 이 트랜잭션에서 조회한다. 트랜잭션 밖에서 조회하면 메시지마다 커넥션을 두 번 빌려 부하 때 풀 대기가 겹치고,
    // 같은 영속성 컨텍스트라 BEFORE_COMMIT 의 알림 저장이 다시 조회하는 발신자는 1차 캐시에서 끝난다.
    @Transactional
    public ChatMessage writeText(Long chatRoomId, Long senderId, String content, Long sequence) {
        UserInfo sender = userQueryApi.getInfo(senderId);
        ChatMessage message = chatMessageRepository.save(
                ChatMessage.text(chatRoomRepository.getReference(chatRoomId), sender.userId(), content, sequence));
        eventPublisher.publishEvent(ChatMessageBroadcastEvent.of(message, sender.nickName(), sender.profileImageUrl()));
        eventPublisher.publishEvent(
                new ChatMessageSentEvent(chatRoomId, message.getId(), sender.userId(), message.getContent()));
        return message;
    }
}
