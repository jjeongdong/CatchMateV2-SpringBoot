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
        Long sequence = chatSequencePort.generateSequence(chatRoomId);

        chatRoom.updateLastMessageSequence(sequence);
        chatRoomRepository.save(chatRoom);

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

        chatRoom.updateLastMessageSequence(sequence);
        chatRoomRepository.save(chatRoom);

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

    public List<ChatMessage> getChatHistory(Long roomId, Long lastMessageId, int size) {
        return chatMessageRepository.findChatHistory(roomId, lastMessageId, size);
    }

    public List<ChatMessage> getSyncMessages(Long roomId, Long lastMessageId, int size) {
        return chatMessageRepository.findSyncMessages(roomId, lastMessageId, size);
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
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));
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

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_NOT_FOUND));
    }
}
