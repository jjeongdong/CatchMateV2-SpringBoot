package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.dto.command.ChatMessageCommand;
import com.back.catchmate.chat.application.event.ChatMessageEvent;
import com.back.catchmate.chat.application.port.in.ChatClientCommandUseCase;
import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;
import com.back.catchmate.chat.application.port.out.external.ImageUploaderPort;
import com.back.catchmate.chat.application.port.out.external.UserFetchPort;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.common.upload.UploadFile;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatClientCommandService implements ChatClientCommandUseCase {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final UserFetchPort userFetchPort;
    private final ImageUploaderPort imageUploaderPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    // DB 커넥션을 메시지 INSERT(+Outbox 저장)에만 잡히게 하려고 트랜잭션을 걸지 않는다(NOT_SUPPORTED).
    // 준비(멤버십 캐시·시퀀스)와 후처리(버퍼링·캐시 evict)는 Redis 로 트랜잭션 밖에서 처리하고,
    // INSERT + 이벤트 발행만 chatMessageService.persistAndPublish 의 좁은 트랜잭션에서 수행한다.
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendMessage(Long senderId, ChatMessageCommand command) {
        ChatUserInfo sender = userFetchPort.getUser(senderId);

        Long sequence = chatMessageService.prepareSequence(
                command.chatRoomId(), senderId, command.messageType());

        chatMessageService.persistAndPublish(
                command.chatRoomId(), senderId, command.content(), command.messageType(), sequence, sender);

        chatMessageService.bufferAfterSend(
                command.chatRoomId(), senderId, sequence, command.messageType());
    }

    @Override
    public void enterChatRoom(Long userId, Long chatRoomId) {
        // 시스템 메시지는 멤버 추가 시 자동 발송되므로 별도 처리 없음
    }

    @Override
    public void leaveChatRoom(Long userId, Long chatRoomId) {
        ChatUserInfo user = userFetchPort.getUser(userId);
        ChatMessage savedMessage = chatRoomService.leaveChatRoom(chatRoomId, user);
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(savedMessage, user));
    }

    @Override
    public void readChatRoom(Long userId, Long chatRoomId) {
        chatMessageService.markAsRead(chatRoomId, userId);
    }

    @Override
    public void updateNotificationSetting(Long userId, Long roomId, boolean isOn) {
        chatRoomMemberService.updateNotificationSetting(roomId, userId, isOn);
    }

    @Override
    public void updateChatRoomImage(Long userId, Long roomId, UploadFile uploadFile) {
        String imageUrl = null;

        if (uploadFile != null) {
            imageUrl = imageUploaderPort.upload(
                    uploadFile.originalFilename(),
                    uploadFile.contentType(),
                    uploadFile.inputStream(),
                    uploadFile.size()
            );
        }

        chatRoomService.updateChatRoomImage(roomId, userId, imageUrl);
    }

    @Override
    public void kickChatRoomMember(Long hostId, Long chatRoomId, Long targetUserId) {
        ChatMessage savedMessage = chatRoomService.kickChatRoomMember(chatRoomId, hostId, targetUserId);

        ChatUserInfo targetUser = userFetchPort.getUser(savedMessage.getSenderId());
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(savedMessage, targetUser));
    }
}
