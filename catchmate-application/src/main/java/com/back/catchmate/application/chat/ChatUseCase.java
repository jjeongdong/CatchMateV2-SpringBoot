package com.back.catchmate.application.chat;

import com.back.catchmate.application.chat.dto.command.ChatMessageCommand;
import com.back.catchmate.application.chat.dto.response.ChatMessageResponse;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.model.MessageType;
import com.back.catchmate.domain.chat.service.ChatMessageService;
import com.back.catchmate.domain.chat.service.ChatRoomService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatUseCase {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    /**
     * 채팅 메시지 저장
     */
    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageCommand command) {
        ChatRoom chatRoom = chatRoomService.getChatRoom(command.getChatRoomId());
        User sender = userService.getUser(command.getSenderId());

        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                sender,
                command.getContent(),
                command.getMessageType()
        );

        ChatMessage savedMessage = chatMessageService.save(chatMessage);
        return ChatMessageResponse.from(savedMessage);
    }

    /**
     * 채팅방의 모든 메시지 조회 (페이징)
     */
    public DomainPage<ChatMessage> getMessages(Long chatRoomId, DomainPageable pageable) {
        return chatMessageService.findAllByChatRoomId(chatRoomId, pageable);
    }

    /**
     * 채팅방의 마지막 메시지 조회
     */
    public ChatMessageResponse getLastMessage(Long chatRoomId) {
        return chatMessageService.findLastMessageByChatRoomId(chatRoomId)
                .map(ChatMessageResponse::from)
                .orElse(null);
    }

    /**
     * 사용자가 특정 채팅방에 접근 권한이 있는지 확인
     *
     * @param userId 사용자 ID
     * @param chatRoomId 채팅방 ID
     * @return 참가자이면 true, 아니면 false
     */
    public boolean canAccessChatRoom(Long userId, Long chatRoomId) {
        return chatRoomService.isUserParticipant(userId, chatRoomId);
    }

    /**
     * 채팅방 입장 메시지 생성 및 저장
     * 서버가 입장 메시지를 자동으로 생성
     */
    @Transactional
    public ChatMessageResponse enterChatRoom(Long userId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomService.getChatRoom(chatRoomId);
        User user = userService.getUser(userId);

        // 서버에서 입장 메시지 생성
        String enterMessage = user.getNickName() + "님이 입장하셨습니다.";

        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                user,
                enterMessage,
                MessageType.SYSTEM
        );

        ChatMessage savedMessage = chatMessageService.save(chatMessage);
        return ChatMessageResponse.from(savedMessage);
    }

    /**
     * 채팅방 퇴장 메시지 생성 및 저장
     * 서버가 퇴장 메시지를 자동으로 생성
     */
    @Transactional
    public ChatMessageResponse leaveChatRoom(Long userId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomService.getChatRoom(chatRoomId);
        User user = userService.getUser(userId);

        // 서버에서 퇴장 메시지 생성
        String leaveMessage = user.getNickName() + "님이 퇴장하셨습니다.";

        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoom,
                user,
                leaveMessage,
                MessageType.SYSTEM
        );

        ChatMessage savedMessage = chatMessageService.save(chatMessage);
        return ChatMessageResponse.from(savedMessage);
    }
}
