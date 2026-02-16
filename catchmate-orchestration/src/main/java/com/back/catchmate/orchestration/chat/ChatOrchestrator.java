package com.back.catchmate.orchestration.chat;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.chat.event.ChatNotificationEvent;
import com.back.catchmate.application.chat.service.ChatService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.chat.dto.command.ChatMessageCommand;
import com.back.catchmate.orchestration.chat.dto.response.ChatMessageResponse;
import com.back.catchmate.orchestration.chat.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.orchestration.chat.dto.response.ChatRoomResponse;
import com.back.catchmate.orchestration.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatOrchestrator {
    private final ChatService chatService;
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void sendMessage(Long senderId, ChatMessageCommand command) {
        User sender = userService.getUser(senderId);

        ChatMessage savedMessage = chatService.saveMessage(
                command.getChatRoomId(),
                sender,
                command.getContent(),
                command.getMessageType()
        );

        applicationEventPublisher.publishEvent(ChatMessageEvent.from(savedMessage));

        // 채팅방 멤버 중 발신자를 제외한 모든 사용자에게 알림 이벤트 발행
        // FCM 알림은 별도의 이벤트 리스너에서 처리
        List<User> recipients = chatService.getChatRoomMembers(savedMessage.getChatRoom().getId())
                .stream()
                .map(ChatRoomMember::getUser)
                .filter(user -> !user.getId().equals(senderId))
                .toList();

        // 알림을 받을 사용자가 있을 때만 이벤트 발행
        if (!recipients.isEmpty()) {
            applicationEventPublisher.publishEvent(ChatNotificationEvent.of(savedMessage, recipients));
        }
    }

    @Transactional
    public void enterChatRoom(Long userId, Long chatRoomId) {
        User user = userService.getUser(userId);
        ChatMessage savedMessage = chatService.enterChatRoom(chatRoomId, user);
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(savedMessage));
    }

    @Transactional
    public void leaveChatRoom(Long userId, Long chatRoomId) {
        User user = userService.getUser(userId);
        ChatMessage savedMessage = chatService.leaveChatRoom(chatRoomId, user);
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(savedMessage));
    }

    public PagedResponse<ChatRoomResponse> getMyChatRooms(Long userId, int page, int size) {
        DomainPageable pageable = new DomainPageable(page, size);
        DomainPage<ChatRoom> chatRoomPage = chatService.getMyChatRooms(userId, pageable);

        List<ChatRoomResponse> responses = chatRoomPage.getContent().stream()
                .map(chatRoom -> {
                    ChatMessageResponse lastMessage = chatService.getLastMessage(chatRoom.getId())
                            .map(ChatMessageResponse::from)
                            .orElse(null);

                    ChatRoomMember member = chatService.getChatRoomMember(chatRoom.getId(), userId); // *Service에 해당 메서드 필요
                    long unreadCount = member.calculateUnreadCount(chatRoom.getLastMessageSequence());
                    return ChatRoomResponse.from(chatRoom, lastMessage, unreadCount);
                })
                .toList();

        return new PagedResponse<>(chatRoomPage, responses);
    }

    @Transactional
    public void readChatRoom(Long userId, Long chatRoomId) {
        chatService.markAsRead(chatRoomId, userId);
    }

    public List<ChatMessageResponse> getChatHistory(Long userId, Long roomId, Long lastMessageId, int size) {
        // 1. 해당 채팅방의 멤버인지 권한 검증
        chatService.validateUserInChatRoom(userId, roomId);

        // 2. 채팅 메시지 조회
        List<ChatMessage> messages = chatService.getChatHistory(roomId, lastMessageId, size);

        // 3. 응답 DTO로 변환하여 반환
        return messages.stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    public List<ChatMessageResponse> syncMessages(Long userId, Long roomId, Long lastMessageId, int size) {
        // 1. 해당 채팅방의 멤버인지 권한 검증
        chatService.validateUserInChatRoom(userId, roomId);

        // 2. 누락된 동기화 메시지 조회
        List<ChatMessage> syncMessages = chatService.getSyncMessages(roomId, lastMessageId, size);

        // 3. 응답 DTO로 변환하여 반환
        return syncMessages.stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    public ChatMessageResponse getLastMessage(Long chatRoomId) {
        return chatService.getLastMessage(chatRoomId)
                .map(ChatMessageResponse::from)
                .orElse(null);
    }

    public List<ChatRoomMemberResponse> getChatRoomMembers(Long chatRoomId) {
        List<ChatRoomMember> activeMembers = chatService.getChatRoomMembers(chatRoomId);

        return activeMembers.stream()
                .map(ChatRoomMemberResponse::from)
                .toList();
    }
}
