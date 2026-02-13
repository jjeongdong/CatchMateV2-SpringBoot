package com.back.catchmate.application.chat.service;

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

        // 1. 방의 시퀀스 증가 및 저장
        // TODO: 시퀀스 증가와 메시지 저장을 원자적으로 처리하기 위해서는 DB 트랜잭션과 락이 필요할 수 있음
        chatRoom.increaseSequence();
        chatRoomRepository.save(chatRoom);

        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                sender,
                content,
                messageType,
                sequence
        );

        updateReadSequence(chatRoom, sender);
        return chatMessageRepository.save(chatMessage);
    }

    public void markAsRead(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        User user = User.builder().id(userId).build(); // ID만 있는 User 객체 (조회 비용 절감)

        updateReadSequence(chatRoom, user);
    }

    /**
     *  내부 헬퍼 메서드: 회원의 lastReadSequence를 방의 lastMessageSequence로 업데이트
     */
    private void updateReadSequence(ChatRoom chatRoom, User user) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), user.getId())
                .ifPresent(member -> {
                    if (member.isActive()) {
                        member.updateLastReadSequence(chatRoom.getLastMessageSequence());
                        chatRoomMemberRepository.save(member);
                    }
                });
    }

    public ChatMessage enterChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        Long sequence = chatSequencePort.generateSequence(chatRoomId);

//        // 멤버 추가 로직 (기존 Domain Service 로직 통합)
//        Optional<ChatRoomMember> existing = chatRoomMemberRepository
//                .findByChatRoomIdAndUserId(chatRoomId, user.getId());
//
//        if (existing.isEmpty() || !existing.get().isActive()) {
//            ChatRoomMember newMember = ChatRoomMember.create(chatRoom, user);
//            chatRoomMemberRepository.save(newMember);
//        }

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
        Long sequence = chatSequencePort.generateSequence(chatRoomId);

        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, user.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        member.leave();
        chatRoomMemberRepository.save(member);

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

    public List<ChatMessage> getChatHistory(Long roomId, Long lastMessageId, int size) {
        return chatMessageRepository.findChatHistory(roomId, lastMessageId, size);
    }

    public DomainPage<ChatMessage> getMessages(Long chatRoomId, DomainPageable pageable) {
        return chatMessageRepository.findAllByChatRoomId(chatRoomId, pageable);
    }

    public Optional<ChatMessage> getLastMessage(Long chatRoomId) {
        return chatMessageRepository.findLastMessageByChatRoomId(chatRoomId);
    }

    public List<ChatRoomMember> getChatRoomMembers(Long chatRoomId) {
        // 존재 확인
        getChatRoom(chatRoomId);
        return chatRoomMemberRepository.findAllByChatRoomIdAndActive(chatRoomId);
    }

    public ChatRoomMember getChatRoomMember(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .filter(ChatRoomMember::isActive) // 탈퇴한 멤버는 조회되지 않도록 필터링
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));
    }

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_NOT_FOUND));
    }

    public void validateUserInChatRoom(Long userId, Long roomId) {
        boolean isMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(roomId, userId)
                .filter(ChatRoomMember::isActive)
                .isPresent();

        if (!isMember) {
            throw new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND);
        }
    }
}
