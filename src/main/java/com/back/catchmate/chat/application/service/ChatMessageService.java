package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.dto.ChatMessageCacheDto;
import com.back.catchmate.chat.application.dto.ChatMessageListDto;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.chat.application.port.out.ChatHistoryCachePort;
import com.back.catchmate.chat.application.port.out.ChatMessageBufferPort;
import com.back.catchmate.chat.application.port.out.ChatRoomSequenceBufferPort;
import com.back.catchmate.chat.application.port.out.ChatSequencePort;
import com.back.catchmate.chat.application.port.out.ReadSequenceBufferPort;
import com.back.catchmate.chat.application.port.out.ChatMessageRepository;
import com.back.catchmate.chat.application.port.out.ChatRoomMemberRepository;
import com.back.catchmate.chat.application.port.out.ChatRoomRepository;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatSequencePort chatSequencePort;
    private final ChatHistoryCachePort chatHistoryCachePort;
    private final ChatMessageBufferPort chatMessageBufferPort;
    private final ReadSequenceBufferPort readSequenceBufferPort;
    private final ChatRoomSequenceBufferPort chatRoomSequenceBufferPort;

    public ChatMessage saveMessage(Long chatRoomId, User sender, String content, MessageType messageType) {
        if (messageType == MessageType.TEXT) {
            ChatRoomMember member = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoomId, sender.getId())
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
            readSequenceBufferPort.buffer(chatRoomId, sender.getId(), sequence);
        } else {
            sequence = chatSequencePort.getCurrentSequence(chatRoomId);
        }

        ChatMessage chatMessage = ChatMessage.createMessage(
                ChatRoom.builder().id(chatRoomId).build(),
                sender,
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

    public void flushReadSequences() {
        Map<String, Long> buffered = readSequenceBufferPort.drainAll();

        for (Map.Entry<String, Long> entry : buffered.entrySet()) {
            String[] parts = entry.getKey().split(":");
            Long chatRoomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            Long sequence = entry.getValue();

            chatRoomMemberRepository.updateLastReadSequenceDirectly(chatRoomId, userId, sequence);
        }

        if (!buffered.isEmpty()) {
            log.debug("읽음 시퀀스 {} 건 DB 반영 완료", buffered.size());
        }
    }

    public void flushMessages() {
        List<ChatMessage> messages = chatMessageBufferPort.drainAll();
        if (!messages.isEmpty()) {
            chatMessageRepository.saveAll(messages);
            log.debug("채팅 메시지 {} 건 배치 DB 반영 완료", messages.size());
        }

        Map<Long, Long> sequences = chatRoomSequenceBufferPort.drainAll();
        for (Map.Entry<Long, Long> entry : sequences.entrySet()) {
            chatRoomRepository.updateMaxSequence(entry.getKey(), entry.getValue());
        }

        if (!sequences.isEmpty()) {
            log.debug("채팅방 시퀀스 {} 건 DB 반영 완료", sequences.size());
        }
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

        List<ChatMessageCacheDto> chatMessageCacheDtoList = merged.stream()
                .map(ChatMessageCacheDto::from)
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
