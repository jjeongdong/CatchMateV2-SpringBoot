package com.back.catchmate.application.chat.service;

import com.back.catchmate.application.chat.dto.ChatMessageCacheDto;
import com.back.catchmate.application.chat.dto.ChatMessageListDto;
import com.back.catchmate.chat.enums.MessageType;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.domain.chat.port.ChatSequencePort;
import com.back.catchmate.domain.chat.repository.ChatMessageRepository;
import com.back.catchmate.domain.chat.repository.ChatRoomMemberRepository;
import com.back.catchmate.domain.chat.repository.ChatRoomRepository;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatSequencePort chatSequencePort;

    public ChatRoom getOrCreateChatRoom(Board board) {
        return chatRoomRepository.findByBoardId(board.getId())
                .orElseGet(() -> {
                    ChatRoom newChatRoom = ChatRoom.createChatRoom(board);
                    return chatRoomRepository.save(newChatRoom);
                });
    }

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
        updateReadSequence(chatRoom, sender.getId());
        return chatMessageRepository.save(chatMessage);
    }

    public void markAsRead(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        updateReadSequence(chatRoom, userId);
    }

    private void updateReadSequence(ChatRoom chatRoom, Long userId) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), userId)
                .ifPresent(member -> {
                    if (member.isActive()) {
                        member.updateLastReadSequence(chatRoom.getLastMessageSequence());
                        chatRoomMemberRepository.save(member);
                    }
                });
    }

    public ChatMessage enterChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatRoom.getLastMessageSequence();

        String enterMessage = user.getNickName() + "님이 입장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                user,
                enterMessage,
                MessageType.SYSTEM,
                sequence
        );
        return chatMessageRepository.save(chatMessage);
    }

    public ChatMessage leaveChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatRoom.getLastMessageSequence();

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
        return chatMessageRepository.save(chatMessage);
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

    public ChatMessage kickChatRoomMember(Long chatRoomId, Long hostId, Long targetUserId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatRoom.getLastMessageSequence();

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

        return chatMessageRepository.save(chatMessage);
    }
}
