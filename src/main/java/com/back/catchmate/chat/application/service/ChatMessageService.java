package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.dto.ChatMessageCacheDto;
import com.back.catchmate.chat.application.dto.ChatMessageListDto;
import com.back.catchmate.chat.application.event.ChatMessageEvent;
import com.back.catchmate.chat.application.event.ChatMessageSentEvent;
import com.back.catchmate.chat.application.port.out.ChatHistoryCachePort;
import com.back.catchmate.chat.application.port.out.ChatMembershipCachePort;
import com.back.catchmate.chat.application.port.out.ChatMembershipCachePort.MembershipSnapshot;
import com.back.catchmate.chat.application.port.out.ChatRoomSequenceBufferPort;
import com.back.catchmate.chat.application.port.out.ChatSequencePort;
import com.back.catchmate.chat.application.port.out.ReadSequenceBufferPort;
import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;
import com.back.catchmate.chat.application.port.out.external.UserFetchPort;
import com.back.catchmate.chat.application.port.out.persistence.ChatMessageRepository;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomMemberRepository;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomRepository;
import com.back.catchmate.chat.application.port.out.persistence.ReadSequenceUpdate;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;

    private final ChatHistoryCachePort chatHistoryCachePort;
    private final ChatMembershipCachePort chatMembershipCachePort;
    private final ChatRoomSequenceBufferPort chatRoomSequenceBufferPort;
    private final ChatSequencePort chatSequencePort;
    private final ReadSequenceBufferPort readSequenceBufferPort;

    private final UserFetchPort userFetchPort;
    private final ChatBufferFlushExecutor chatBufferFlushExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * [트랜잭션 밖] 멤버십 인증(캐시) + 시퀀스 발행(Redis). DB 커넥션을 잡지 않도록 NOT_SUPPORTED.
     * TEXT 만 새 시퀀스를 INCR 하고, 그 외(이미지 등)는 현재 시퀀스를 읽는다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long prepareSequence(Long chatRoomId, Long senderId, MessageType messageType) {
        if (messageType == MessageType.TEXT) {
            MembershipSnapshot membership = resolveMembership(chatRoomId, senderId);
            if (!membership.active()) {
                throw new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND);
            }
            if (membership.readOnly()) {
                throw new BaseException(ErrorCode.CHATROOM_READ_ONLY);
            }
            return chatSequencePort.generateSequence(chatRoomId);
        }
        return chatSequencePort.getCurrentSequence(chatRoomId);
    }

    /**
     * [좁은 트랜잭션] 메시지 INSERT + 이벤트 발행.
     * Outbox 2단계(절대 변경 금지)를 위해 INSERT 와 ChatMessageSentEvent 발행은 반드시 같은 트랜잭션이어야
     * @EventListener(커밋 전 Outbox 저장)가 원자적으로 커밋된다. 브로드캐스트/알림 dispatch 는 AFTER_COMMIT.
     */
    @Transactional
    public ChatMessage persistAndPublish(Long chatRoomId, Long senderId, String content,
                                         MessageType messageType, Long sequence, ChatUserInfo sender) {
        ChatMessage chatMessage = ChatMessage.createMessage(chatRoomId, senderId, content, messageType, sequence);
        chatMessage = chatMessageRepository.save(chatMessage);

        applicationEventPublisher.publishEvent(ChatMessageEvent.from(chatMessage, sender));
        applicationEventPublisher.publishEvent(ChatMessageSentEvent.of(
                chatMessage.getChatRoomId(),
                chatMessage.getId(),
                senderId,
                chatMessage.getContent()
        ));
        return chatMessage;
    }

    /**
     * [트랜잭션 밖] 커밋 후 후처리(Redis): 시퀀스 버퍼링 + 히스토리 캐시 evict. DB 커넥션 불필요.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void bufferAfterSend(Long chatRoomId, Long senderId, Long sequence, MessageType messageType) {
        if (messageType == MessageType.TEXT) {
            chatRoomSequenceBufferPort.buffer(chatRoomId, sequence);
            readSequenceBufferPort.buffer(chatRoomId, senderId, sequence);
        }
        chatHistoryCachePort.evictLatestPage(chatRoomId);
    }

    // 멤버십 인증 캐시(read-through). miss 시에만 DB 조회 후 캐시 적재. 멤버 행이 없으면 예외.
    private MembershipSnapshot resolveMembership(Long chatRoomId, Long userId) {
        return chatMembershipCachePort.find(chatRoomId, userId)
                .orElseGet(() -> {
                    ChatRoomMember member = chatRoomMemberRepository
                            .findByChatRoomIdAndUserId(chatRoomId, userId)
                            .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));
                    MembershipSnapshot snapshot = new MembershipSnapshot(member.isActive(), member.isReadOnly());
                    chatMembershipCachePort.put(chatRoomId, userId, snapshot);
                    return snapshot;
                });
    }

    public void markAsRead(Long chatRoomId, Long userId) {
        try {
            Long lastSequence = chatSequencePort.getCurrentSequence(chatRoomId);
            readSequenceBufferPort.buffer(chatRoomId, userId, lastSequence);
        } catch (Exception e) {
            log.error("읽음 처리 버퍼링 중 오류 발생 (roomId: {}, userId: {})", chatRoomId, userId, e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void flushReadSequences() {
        List<ReadSequenceUpdate> updates = readSequenceBufferPort.drainAll();
        if (updates.isEmpty()) {
            return;
        }

        chatBufferFlushExecutor.flushReadSequences(updates);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void flushRoomSequences() {
        Map<Long, Long> sequences = chatRoomSequenceBufferPort.drainAll();

        if (sequences.isEmpty()) {
            return;
        }

        chatBufferFlushExecutor.flushRoomSequences(sequences);
    }

    @Cacheable(
            value = "chatHistory",
            key = "#roomId + '_' + (#lastMessageId != null ? #lastMessageId : 'START') + '_' + #size",
            cacheManager = "redisCacheManager"
    )
    public ChatMessageListDto getChatHistory(Long roomId, Long lastMessageId, int size) {
        List<ChatMessage> dbMessages = chatMessageRepository.findChatHistory(roomId, lastMessageId, size);

        List<Long> senderIds = dbMessages.stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .toList();
        Map<Long, ChatUserInfo> senderById = senderIds.isEmpty()
                ? Map.of()
                : userFetchPort.getUsers(senderIds).stream()
                        .collect(Collectors.toMap(ChatUserInfo::userId, Function.identity()));

        List<ChatMessageCacheDto> chatMessageCacheDtoList = dbMessages.stream()
                .map(msg -> ChatMessageCacheDto.from(msg, senderById.get(msg.getSenderId())))
                .toList();

        return new ChatMessageListDto(chatMessageCacheDtoList);
    }

    public List<ChatMessage> getSyncMessages(Long roomId, Long lastMessageId, int size) {
        return chatMessageRepository.findSyncMessages(roomId, lastMessageId, size);
    }

    public Optional<ChatMessage> getLastMessage(Long chatRoomId) {
        return chatMessageRepository.findLastTextMessageByChatRoomId(chatRoomId);
    }

    public Map<Long, ChatMessage> getLastMessagesByChatRoomIds(List<Long> chatRoomIds) {
        return chatMessageRepository.findLastTextMessagesByChatRoomIds(chatRoomIds);
    }
}
