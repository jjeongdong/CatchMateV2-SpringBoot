package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.event.ChatMessageEvent;
import com.back.catchmate.chat.application.port.in.ChatInternalCommandUseCase;
import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;
import com.back.catchmate.chat.application.port.out.external.UserFetchPort;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.chat.domain.model.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatInternalCommandService implements ChatInternalCommandUseCase {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final UserFetchPort userFetchPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Long getOrCreateChatRoom(Long boardId) {
        return chatRoomService.getOrCreateChatRoom(boardId).getId();
    }

    @Override
    public void addMember(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomService.getChatRoom(chatRoomId);
        chatRoomMemberService.addMember(chatRoom, userId);
    }

    @Override
    public void welcomeNewMember(Long chatRoomId, Long userId) {
        ChatUserInfo user = userFetchPort.getUser(userId);
        ChatMessage joinMessage = chatRoomService.enterChatRoom(chatRoomId, user);
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(joinMessage, user));
    }

    @Override
    public void addBoardChatRoomMember(Long boardId, Long userId) {
        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(boardId);
        chatRoomMemberService.addMember(chatRoom, userId);
        welcomeNewMember(chatRoom.getId(), userId);
    }

    @Override
    public void flushReadSequences() {
        chatMessageService.flushReadSequences();
    }

    @Override
    public void flushRoomSequences() {
        chatMessageService.flushRoomSequences();
    }
}
