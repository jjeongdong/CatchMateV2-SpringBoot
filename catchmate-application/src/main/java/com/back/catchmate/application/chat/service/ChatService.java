package com.back.catchmate.application.chat.service;

import com.back.catchmate.application.chat.dto.ChatMessageCacheDto;
import com.back.catchmate.application.chat.dto.ChatMessageListDto;
import com.back.catchmate.chat.enums.MessageType;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.domain.chat.port.ChatHistoryCachePort;
import com.back.catchmate.domain.chat.port.ChatSequencePort;
import com.back.catchmate.domain.chat.port.ReadSequenceBufferPort;
import com.back.catchmate.domain.chat.repository.ChatMessageRepository;
import com.back.catchmate.domain.chat.repository.ChatRoomMemberRepository;
import com.back.catchmate.domain.chat.repository.ChatRoomRepository;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatSequencePort chatSequencePort;
    private final ChatHistoryCachePort chatHistoryCachePort;
    private final ReadSequenceBufferPort luaBuffer;
    private final ReadSequenceBufferPort javaBuffer;
    private final MeterRegistry meterRegistry;

    @Value("${chat.read-sequence.mode:V4_LUA_BUFFERED}")
    private ReadSequenceMode mode;

    private Counter readEventsCounter;
    private Counter dbUpdatesCounter;
    private Timer flushTimer;

    public ChatService(
            ChatRoomRepository chatRoomRepository,
            ChatMessageRepository chatMessageRepository,
            ChatRoomMemberRepository chatRoomMemberRepository,
            ChatSequencePort chatSequencePort,
            ChatHistoryCachePort chatHistoryCachePort,
            @Qualifier("redisReadSequenceBufferAdapter") ReadSequenceBufferPort luaBuffer,
            @Qualifier("javaReadSequenceBufferAdapter") ReadSequenceBufferPort javaBuffer,
            MeterRegistry meterRegistry) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.chatSequencePort = chatSequencePort;
        this.chatHistoryCachePort = chatHistoryCachePort;
        this.luaBuffer = luaBuffer;
        this.javaBuffer = javaBuffer;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initMetrics() {
        String modeTag = mode.name();
        this.readEventsCounter = Counter.builder("chat.read.events")
                .description("Number of markAsRead invocations")
                .tag("mode", modeTag)
                .register(meterRegistry);
        this.dbUpdatesCounter = Counter.builder("chat.read.db.updates")
                .description("Rows UPDATEd to chat_room_members for last_read_sequence")
                .tag("mode", modeTag)
                .register(meterRegistry);
        this.flushTimer = Timer.builder("chat.read.flush.duration")
                .description("Duration of one flushReadSequences cycle")
                .tag("mode", modeTag)
                .publishPercentileHistogram()
                .register(meterRegistry);
        Gauge.builder("chat.read.buffer.size", this, ChatService::activeBufferSize)
                .description("Current entries in the active read-sequence buffer")
                .tag("mode", modeTag)
                .strongReference(true)
                .register(meterRegistry);
    }

    private double activeBufferSize() {
        return switch (mode) {
            case V3_JAVA_BUFFERED -> javaBuffer.size();
            case V4_LUA_BUFFERED -> luaBuffer.size();
            default -> 0.0;
        };
    }

    @Transactional
    public ChatMessage saveMessage(Long chatRoomId, User sender, String content, MessageType messageType) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatSequencePort.generateSequence(chatRoomId);

        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                sender,
                content,
                messageType,
                sequence
        );
        applyReadSequence(chatRoomId, sender.getId(), sequence);
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return saved;
    }

    public void markAsRead(Long chatRoomId, Long userId) {
        readEventsCounter.increment();
        try {
            Long lastSequence = resolveLastSequence(chatRoomId);
            applyReadSequence(chatRoomId, userId, lastSequence);
        } catch (Exception e) {
            log.error("읽음 처리 중 오류 발생 (roomId: {}, userId: {})", chatRoomId, userId, e);
        }
    }

    private Long resolveLastSequence(Long chatRoomId) {
        return chatSequencePort.getCurrentSequence(chatRoomId);
    }

    private void applyReadSequence(Long chatRoomId, Long userId, Long sequence) {
        switch (mode) {
            case V1_DIRTY_CHECK -> applyV1DirtyCheck(chatRoomId, userId, sequence);
            case V2_DIRECT_UPDATE -> {
                chatRoomMemberRepository.updateLastReadSequenceDirectly(chatRoomId, userId, sequence);
                dbUpdatesCounter.increment();
            }
            case V3_JAVA_BUFFERED -> javaBuffer.buffer(chatRoomId, userId, sequence);
            case V4_LUA_BUFFERED -> luaBuffer.buffer(chatRoomId, userId, sequence);
        }
    }

    private void applyV1DirtyCheck(Long chatRoomId, Long userId, Long sequence) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .ifPresent(member -> {
                    if (member.isActive()) {
                        member.updateLastReadSequence(sequence);
                        chatRoomMemberRepository.save(member);
                        dbUpdatesCounter.increment();
                    }
                });
    }

    public void flushReadSequences() {
        ReadSequenceBufferPort buffer = switch (mode) {
            case V3_JAVA_BUFFERED -> javaBuffer;
            case V4_LUA_BUFFERED -> luaBuffer;
            default -> null;
        };
        if (buffer == null) return;

        Timer.Sample sample = Timer.start(meterRegistry);
        Map<String, Long> buffered = buffer.drainAll();

        if (!buffered.isEmpty()) {
            List<Map.Entry<String, Long>> updates = new ArrayList<>(buffered.entrySet());
            chatRoomMemberRepository.updateLastReadSequenceBatch(updates);
            dbUpdatesCounter.increment(buffered.size());
            log.debug("읽음 시퀀스 {} 건 DB 일괄 반영(Batch Update) 완료", buffered.size());
        }

        sample.stop(flushTimer);
    }

    public ChatMessage enterChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = currentMessageSequence(chatRoom);

        String enterMessage = user.getNickName() + "님이 입장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                user,
                enterMessage,
                MessageType.SYSTEM,
                sequence
        );
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return saved;
    }

    @CacheEvict(value = "chatRoomMemberAuth", key = "#chatRoomId + '_' + #user.id", cacheManager = "redisCacheManager")
    public ChatMessage leaveChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);

        ChatRoomMember chatRoomMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, user.getId())
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        Long sequence = currentMessageSequence(chatRoom);

        chatRoomMember.leave();
        chatRoomMemberRepository.save(chatRoomMember);

        String leaveMessage = user.getNickName() + "님이 퇴장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                user,
                leaveMessage,
                MessageType.SYSTEM,
                sequence
        );
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return saved;
    }

    private Long currentMessageSequence(ChatRoom chatRoom) {
        return chatSequencePort.getCurrentSequence(chatRoom.getId());
    }

    public DomainPage<ChatRoom> getMyChatRooms(Long userId, DomainPageable pageable) {
        return chatRoomRepository.findAllByUserId(userId, pageable);
    }

    public Map<Long, Long> getBufferedReadSequences(List<Long> chatRoomIds, Long userId) {
        return switch (mode) {
            case V3_JAVA_BUFFERED -> javaBuffer.getBufferedSequences(chatRoomIds, userId);
            case V4_LUA_BUFFERED -> luaBuffer.getBufferedSequences(chatRoomIds, userId);
            default -> Map.of();
        };
    }

    @Cacheable(
            value = "chatHistory",
            key = "#roomId + '_' + (#lastMessageId != null ? #lastMessageId : 'START') + '_' + #size",
            cacheManager = "redisCacheManager"
    )
    public ChatMessageListDto getChatHistory(Long roomId, Long lastMessageId, int size) {
        List<ChatMessage> messages = chatMessageRepository.findChatHistory(roomId, lastMessageId, size);

        List<ChatMessageCacheDto> chatMessageCacheDtoList = messages.stream()
                .map(ChatMessageCacheDto::from)
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

    public Map<Long, ChatRoomMember> getChatRoomMembersByChatRoomIds(List<Long> chatRoomIds, Long userId) {
        return chatRoomMemberRepository.findByChatRoomIdsAndUserId(chatRoomIds, userId);
    }

    public List<ChatRoomMember> getChatRoomMembers(Long chatRoomId) {
        // 존재 확인
        getChatRoom(chatRoomId);
        return chatRoomMemberRepository.findAllByChatRoomIdAndActive(chatRoomId);
    }

    public ChatRoomMember getChatRoomMember(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));
    }

    @Cacheable(
            value = "chatRoomMemberAuth",
            key = "#roomId + '_' + #userId",
            cacheManager = "redisCacheManager"
    )
    public boolean validateUserInChatRoom(Long userId, Long roomId) {
        boolean isMember = chatRoomMemberRepository
                .existsByChatRoomIdAndUserIdAndActive(roomId, userId);

        if (!isMember) {
            throw new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND);
        }

        return true;
    }

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_NOT_FOUND));
    }

    public void updateNotificationSetting(Long chatRoomId, Long userId, boolean isOn) {
        ChatRoomMember member = getChatRoomMember(chatRoomId, userId);
        member.updateNotificationSetting(isOn);
        chatRoomMemberRepository.save(member);
    }

    public void updateChatRoomImage(Long chatRoomId, Long userId, String imageUrl) {
        // 1. 해당 채팅방의 멤버인지 권한 검증
        validateUserInChatRoom(userId, chatRoomId);

        // 2. 채팅방 조회 및 이미지 업데이트
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        chatRoom.updateImageUrl(imageUrl);

        // 3. 저장
        chatRoomRepository.save(chatRoom);
    }

    @CacheEvict(value = "chatRoomMemberAuth", key = "#chatRoomId + '_' + #targetUserId", cacheManager = "redisCacheManager")
    public ChatMessage kickChatRoomMember(Long chatRoomId, Long hostId, Long targetUserId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);

        // 1. 방장 권한 검증 (게시글 작성자가 방장이라고 가정)
        if (!chatRoom.getBoard().getUser().getId().equals(hostId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 본인 스스로를 내보낼 수 없음 (자진 퇴장 API 사용 권장)
        if (hostId.equals(targetUserId)) {
            throw new BaseException(ErrorCode.BAD_REQUEST);
        }

        // 3. 내보낼 대상자가 현재 방에 참여 중인지 확인
        ChatRoomMember targetMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        Long sequence = currentMessageSequence(chatRoom);

        // 4. 대상자 퇴장 처리 (leftAt 시간 업데이트)
        targetMember.leave();
        chatRoomMemberRepository.save(targetMember);

        String kickMessage = targetMember.getUser().getNickName() + "님이 내보내졌습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                targetMember.getUser(),
                kickMessage,
                MessageType.SYSTEM,
                sequence
        );

        ChatMessage saved = chatMessageRepository.save(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return saved;
    }
}
