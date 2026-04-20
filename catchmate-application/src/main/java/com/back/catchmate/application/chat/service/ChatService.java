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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatSequencePort chatSequencePort;
    private final ChatHistoryCachePort chatHistoryCachePort;
    private final ReadSequenceBufferPort readSequenceBufferPort;

    @Transactional
    public ChatMessage saveMessage(Long chatRoomId, User sender, String content, MessageType messageType) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatSequencePort.generateSequence(chatRoomId);

        chatRoom.updateLastMessageSequence(sequence);
        chatRoomRepository.save(chatRoom);

        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                sender,
                content,
                messageType,
                sequence
        );
        readSequenceBufferPort.buffer(chatRoomId, sender.getId(), sequence);
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return saved;
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

    public ChatMessage enterChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatSequencePort.generateSequence(chatRoomId);

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
        Long sequence = chatSequencePort.generateSequence(chatRoomId);

        ChatRoomMember chatRoomMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, user.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

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

    public DomainPage<ChatRoom> getMyChatRooms(Long userId, DomainPageable pageable) {
        return chatRoomRepository.findAllByUserId(userId, pageable);
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
                .findByChatRoomIdAndUserId(roomId, userId)
                .filter(ChatRoomMember::isActive)
                .isPresent();

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
        Long sequence = chatSequencePort.generateSequence(chatRoomId);

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
