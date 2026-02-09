package com.back.catchmate.application.chat.service;

import com.back.catchmate.chat.enums.MessageType;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.model.ChatRoomMember;
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

    public ChatRoom getOrCreateChatRoom(Board board) {
        return chatRoomRepository.findByBoardId(board.getId())
                .orElseGet(() -> {
                    ChatRoom newChatRoom = ChatRoom.createChatRoom(board);
                    return chatRoomRepository.save(newChatRoom);
                });
    }

    public ChatMessage saveMessage(Long chatRoomId, User sender, String content, MessageType messageType) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);

        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                sender,
                content,
                messageType
        );
        return chatMessageRepository.save(chatMessage);
    }

    public ChatMessage enterChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);

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
                MessageType.SYSTEM
        );
        return chatMessageRepository.save(chatMessage);
    }

    public ChatMessage leaveChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);

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
                MessageType.SYSTEM
        );
        return chatMessageRepository.save(chatMessage);
    }

    public DomainPage<ChatRoom> getMyChatRooms(Long userId, DomainPageable pageable) {
        return chatRoomRepository.findAllByUserId(userId, pageable);
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

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_NOT_FOUND));
    }
}
