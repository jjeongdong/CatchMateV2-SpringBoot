package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.dto.ChatMessageCacheDto;
import com.back.catchmate.chat.application.dto.ChatMessageListDto;
import com.back.catchmate.chat.application.port.out.ChatHistoryCachePort;
import com.back.catchmate.chat.application.port.out.ChatMessageBufferPort;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final ChatMessageBufferPort chatMessageBufferPort;
    private final ChatRoomSequenceBufferPort chatRoomSequenceBufferPort;
    private final ChatSequencePort chatSequencePort;
    private final ReadSequenceBufferPort readSequenceBufferPort;

    private final UserFetchPort userFetchPort;
    private final ChatBufferFlushExecutor chatBufferFlushExecutor;

    public ChatMessage saveMessage(Long chatRoomId, Long senderId, String content, MessageType messageType) {
        if (messageType == MessageType.TEXT) {
            ChatRoomMember member = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoomId, senderId)
                    .filter(ChatRoomMember::isActive)
                    .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

            if (member.isReadOnly()) {
                throw new BaseException(ErrorCode.CHATROOM_READ_ONLY);
            }
        }

        Long sequence;
        if (messageType == MessageType.TEXT) {
            sequence = chatSequencePort.generateSequence(chatRoomId);
            chatRoomSequenceBufferPort.buffer(chatRoomId, sequence);
            readSequenceBufferPort.buffer(chatRoomId, senderId, sequence);
        } else {
            sequence = chatSequencePort.getCurrentSequence(chatRoomId);
        }

        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoomId,
                senderId,
                content,
                messageType,
                sequence
        );

        chatMessageBufferPort.buffer(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return chatMessage;
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
    public void flushMessages() {
        List<ChatMessage> messages = chatMessageBufferPort.drainAll();
        Map<Long, Long> sequences = chatRoomSequenceBufferPort.drainAll();

        if (messages.isEmpty() && sequences.isEmpty()) {
            return;
        }

        chatBufferFlushExecutor.flushMessages(messages, sequences);
    }

    @Cacheable(
            value = "chatHistory",
            key = "#roomId + '_' + (#lastMessageId != null ? #lastMessageId : 'START') + '_' + #size",
            cacheManager = "redisCacheManager"
    )
    public ChatMessageListDto getChatHistory(Long roomId, Long lastMessageId, int size) {
        List<ChatMessage> dbMessages = chatMessageRepository.findChatHistory(roomId, lastMessageId, size);
        List<ChatMessage> bufferedMessages = chatMessageBufferPort.readByRoomId(roomId);

        List<ChatMessage> merged = mergeHistoryMessages(dbMessages, bufferedMessages, lastMessageId, size);

        List<Long> senderIds = merged.stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .toList();
        Map<Long, ChatUserInfo> senderById = senderIds.isEmpty()
                ? Map.of()
                : userFetchPort.getUsers(senderIds).stream()
                        .collect(Collectors.toMap(ChatUserInfo::userId, Function.identity()));

        List<ChatMessageCacheDto> chatMessageCacheDtoList = merged.stream()
                .map(msg -> ChatMessageCacheDto.from(msg, senderById.get(msg.getSenderId())))
                .toList();

        return new ChatMessageListDto(chatMessageCacheDtoList);
    }

    public List<ChatMessage> getSyncMessages(Long roomId, Long lastMessageId, int size) {
        List<ChatMessage> dbMessages = chatMessageRepository.findSyncMessages(roomId, lastMessageId, size);
        List<ChatMessage> bufferedMessages = chatMessageBufferPort.readByRoomId(roomId);

        if (bufferedMessages.isEmpty()) {
            return dbMessages;
        }

        List<ChatMessage> filtered = bufferedMessages.stream()
                .filter(msg -> lastMessageId == null || msg.getSequence() > lastMessageId)
                .toList();

        if (filtered.isEmpty()) {
            return dbMessages;
        }

        List<ChatMessage> merged = new ArrayList<>(dbMessages);
        merged.addAll(filtered);
        merged.sort(Comparator.comparing(ChatMessage::getSequence));

        return merged.size() > size ? merged.subList(0, size) : merged;
    }

    private List<ChatMessage> mergeHistoryMessages(List<ChatMessage> dbMessages, List<ChatMessage> bufferedMessages,
                                                   Long lastMessageId, int size) {
        if (bufferedMessages.isEmpty()) {
            return dbMessages;
        }

        List<ChatMessage> filtered = bufferedMessages.stream()
                .filter(msg -> lastMessageId == null || msg.getSequence() < lastMessageId)
                .toList();

        if (filtered.isEmpty()) {
            return dbMessages;
        }

        List<ChatMessage> merged = new ArrayList<>(dbMessages);
        merged.addAll(filtered);
        merged.sort(Comparator.comparing(ChatMessage::getSequence).reversed());

        return merged.size() > size ? merged.subList(0, size) : merged;
    }

    public Optional<ChatMessage> getLastMessage(Long chatRoomId) {
        return chatMessageRepository.findLastTextMessageByChatRoomId(chatRoomId);
    }

    public Map<Long, ChatMessage> getLastMessagesByChatRoomIds(List<Long> chatRoomIds) {
        return chatMessageRepository.findLastTextMessagesByChatRoomIds(chatRoomIds);
    }
}
