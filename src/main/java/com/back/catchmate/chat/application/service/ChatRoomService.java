package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.port.out.UserFetchPort;

import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.chat.application.port.out.ChatHistoryCachePort;
import com.back.catchmate.chat.application.port.out.ChatMessageBufferPort;
import com.back.catchmate.chat.application.port.out.ChatSequencePort;
import com.back.catchmate.chat.application.port.out.ChatRoomMemberRepository;
import com.back.catchmate.chat.application.port.out.ChatRoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;

    private final ChatHistoryCachePort chatHistoryCachePort;
    private final ChatMessageBufferPort chatMessageBufferPort;
    private final ChatSequencePort chatSequencePort;

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

    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
    }

    /**
     * 게시글로 채팅방 조회 또는 생성
     * 이미 채팅방이 있으면 기존 채팅방을 반환하고, 없으면 새로 생성
     */
    public ChatRoom getOrCreateChatRoom(Board board) {
        return chatRoomRepository.findByBoardId(board.getId())
                .orElseGet(() -> {
                    ChatRoom newChatRoom = ChatRoom.createChatRoom(board);
                    return chatRoomRepository.save(newChatRoom);
                });
    }

    // 사용자 기준으로 참가중인 채팅방 리스트 조회 (페이징)
    // ChatRoomMember 테이블을 통해 활성 멤버의 채팅방만 조회
    public Page<ChatRoom> findAllByUserId(Long userId, Pageable pageable) {
        return chatRoomRepository.findAllByUserId(userId, pageable);
    }

    // 사용자 기준으로 참가중인 채팅방 리스트 조회
    // ChatRoomMember 테이블을 통해 활성 멤버의 채팅방만 조회
    public List<ChatRoom> findAllByUserId(Long userId) {
        return chatRoomRepository.findAllByUserId(userId);
    }

    public ChatMessage enterChatRoom(Long chatRoomId, User user) {
        Long sequence = chatSequencePort.getCurrentSequence(chatRoomId);

        String enterMessage = user.getNickName() + "님이 입장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                ChatRoom.builder().id(chatRoomId).build(),
                user,
                enterMessage,
                MessageType.SYSTEM,
                sequence
        );

        chatMessageBufferPort.buffer(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return chatMessage;
    }

    @CacheEvict(value = "chatRoomMemberAuth", key = "#chatRoomId + '_' + #user.id", cacheManager = "redisCacheManager")
    @Transactional
    public ChatMessage leaveChatRoom(Long chatRoomId, User user) {
        Long sequence = chatSequencePort.getCurrentSequence(chatRoomId);

        ChatRoomMember chatRoomMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, user.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        chatRoomMember.leave();
        chatRoomMemberRepository.save(chatRoomMember);

        String leaveMessage = user.getNickName() + "님이 퇴장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                ChatRoom.builder().id(chatRoomId).build(),
                user,
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
    @Transactional
    public ChatMessage kickChatRoomMember(Long chatRoomId, Long hostId, Long targetUserId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatSequencePort.getCurrentSequence(chatRoomId);

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
                ChatRoom.builder().id(chatRoomId).build(),
                targetMember.getUser(),
                kickMessage,
                MessageType.SYSTEM,
                sequence
        );

        chatMessageBufferPort.buffer(chatMessage);
        chatHistoryCachePort.evictLatestPage(chatRoomId);
        return chatMessage;
    }
}
