package com.back.catchmate.orchestration.chat;

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
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatOrchestrator {
    private final ChatService chatService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageCommand command) {
        User sender = userService.getUser(senderId);

        ChatMessage savedMessage = chatService.saveMessage(command.getChatRoomId(), sender, command.getContent(), command.getMessageType());

        // 이벤트 발행
        publishChatNotificationEvent(savedMessage, senderId);

        return ChatMessageResponse.from(savedMessage);
    }

    @Transactional
    public ChatMessageResponse enterChatRoom(Long userId, Long chatRoomId) {
        User user = userService.getUser(userId);
        ChatMessage savedMessage = chatService.enterChatRoom(chatRoomId, user);
        return ChatMessageResponse.from(savedMessage);
    }

    @Transactional
    public ChatMessageResponse leaveChatRoom(Long userId, Long chatRoomId) {
        User user = userService.getUser(userId);
        ChatMessage savedMessage = chatService.leaveChatRoom(chatRoomId, user);
        return ChatMessageResponse.from(savedMessage);
    }

    public PagedResponse<ChatRoomResponse> getMyChatRooms(Long userId, int page, int size) {
        DomainPageable pageable = new DomainPageable(page, size);
        DomainPage<ChatRoom> chatRoomPage = chatService.getMyChatRooms(userId, pageable);

        List<ChatRoomResponse> responses = chatRoomPage.getContent().stream()
                .map(chatRoom -> {
                    ChatMessageResponse lastMessage = chatService.getLastMessage(chatRoom.getId())
                            .map(ChatMessageResponse::from)
                            .orElse(null);
                    return ChatRoomResponse.from(chatRoom, lastMessage);
                })
                .toList();

        return new PagedResponse<>(chatRoomPage, responses);
    }

    public PagedResponse<ChatMessageResponse> getMessages(Long chatRoomId, int page, int size) {
        DomainPageable pageable = new DomainPageable(page, size);
        DomainPage<ChatMessage> messagePage = chatService.getMessages(chatRoomId, pageable);

        List<ChatMessageResponse> responses = messagePage.getContent().stream()
                .map(ChatMessageResponse::from)
                .toList();

        return new PagedResponse<>(messagePage, responses);
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

    private void publishChatNotificationEvent(ChatMessage savedMessage, Long senderId) {
        List<User> recipients = chatService.getChatRoomMembers(savedMessage.getChatRoom().getId())
                .stream()
                .map(ChatRoomMember::getUser)
                .filter(user -> !user.getId().equals(senderId))
                .toList();

        if (!recipients.isEmpty()) {
            eventPublisher.publishEvent(ChatNotificationEvent.of(savedMessage, recipients));
        }
    }
}
