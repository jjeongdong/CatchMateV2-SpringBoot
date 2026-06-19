package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.port.out.ChatHistoryCachePort;
import com.back.catchmate.chat.application.port.out.ChatMessageBufferPort;
import com.back.catchmate.chat.application.port.out.ChatSequencePort;
import com.back.catchmate.chat.application.port.out.dto.ChatBoardInfo;
import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;
import com.back.catchmate.chat.application.port.out.external.BoardFetchPort;
import com.back.catchmate.chat.application.port.out.external.UserFetchPort;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomMemberRepository;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomRepository;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;

    private final ChatHistoryCachePort chatHistoryCachePort;
    private final ChatMessageBufferPort chatMessageBufferPort;
    private final ChatSequencePort chatSequencePort;

    private final BoardFetchPort boardFetchPort;
    private final UserFetchPort userFetchPort;

    public ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_NOT_FOUND));
    }

    public Optional<ChatRoom> findById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId);
    }

    public Optional<ChatRoom> findByBoardId(Long boardId) {
        return chatRoomRepository.findByBoardId(boardId);
    }

    @Transactional
    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public ChatRoom getOrCreateChatRoom(Long boardId) {
        return chatRoomRepository.findByBoardId(boardId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.createChatRoom(boardId)));
    }

    public Page<ChatRoom> findAllByUserId(Long userId, Pageable pageable) {
        return chatRoomRepository.findAllByUserId(userId, pageable);
    }

    public List<ChatRoom> findAllByUserId(Long userId) {
        return chatRoomRepository.findAllByUserId(userId);
    }

    @Transactional
    public ChatMessage enterChatRoom(Long chatRoomId, ChatUserInfo user) {
        Long sequence = chatSequencePort.getCurrentSequence(chatRoomId);

        String enterMessage = user.nickName() + "님이 입장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoomId,
                user.userId(),
                enterMessage,
                MessageType.SYSTEM,
                sequence
        );

        chatMessageBufferPort.buffer(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return chatMessage;
    }

    @CacheEvict(value = "chatRoomMemberAuth", key = "#chatRoomId + '_' + #user.userId()", cacheManager = "redisCacheManager")
    @Transactional
    public ChatMessage leaveChatRoom(Long chatRoomId, ChatUserInfo user) {
        Long sequence = chatSequencePort.getCurrentSequence(chatRoomId);

        ChatRoomMember chatRoomMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, user.userId())
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        chatRoomMember.leave();
        chatRoomMemberRepository.save(chatRoomMember);

        String leaveMessage = user.nickName() + "님이 퇴장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoomId,
                user.userId(),
                leaveMessage,
                MessageType.SYSTEM,
                sequence
        );

        chatMessageBufferPort.buffer(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return chatMessage;
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

    @Transactional
    public void updateChatRoomImage(Long chatRoomId, Long userId, String imageUrl) {
        validateUserInChatRoom(userId, chatRoomId);

        ChatRoom chatRoom = getChatRoom(chatRoomId);
        chatRoom.updateImageUrl(imageUrl);

        chatRoomRepository.save(chatRoom);
    }

    @CacheEvict(value = "chatRoomMemberAuth", key = "#chatRoomId + '_' + #targetUserId", cacheManager = "redisCacheManager")
    @Transactional
    public ChatMessage kickChatRoomMember(Long chatRoomId, Long hostId, Long targetUserId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatSequencePort.getCurrentSequence(chatRoomId);

        ChatBoardInfo board = boardFetchPort.getBoard(chatRoom.getBoardId());
        if (!board.userId().equals(hostId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (hostId.equals(targetUserId)) {
            throw new BaseException(ErrorCode.BAD_REQUEST);
        }

        ChatRoomMember targetMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        targetMember.leave();
        chatRoomMemberRepository.save(targetMember);

        ChatUserInfo targetUser = userFetchPort.getUser(targetMember.getUserId());
        String kickMessage = targetUser.nickName() + "님이 내보내졌습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoomId,
                targetUser.userId(),
                kickMessage,
                MessageType.SYSTEM,
                sequence
        );

        chatMessageBufferPort.buffer(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return chatMessage;
    }
}
