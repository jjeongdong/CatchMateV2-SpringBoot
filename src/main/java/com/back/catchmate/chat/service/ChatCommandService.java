package com.back.catchmate.chat.service;

import com.back.catchmate.chat.dto.command.ChatMessageCommand;
import com.back.catchmate.chat.entity.ChatMessage;
import com.back.catchmate.chat.entity.ChatRoom;
import com.back.catchmate.chat.entity.MessageType;
import com.back.catchmate.chat.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.infra.S3ImageUploader;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.upload.UploadFile;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatCommandService {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final UserService userService;
    private final S3ImageUploader s3ImageUploader;
    private final ApplicationEventPublisher applicationEventPublisher;

    // DB 커넥션을 메시지 INSERT(+Outbox 저장)에만 잡히게 하려고 트랜잭션을 걸지 않는다(NOT_SUPPORTED).
    // 준비(멤버십 캐시·시퀀스)와 후처리(버퍼링·캐시 evict)는 Redis 로 트랜잭션 밖에서 처리하고,
    // INSERT + 이벤트 발행만 chatMessageService.persistAndPublish 의 좁은 트랜잭션에서 수행한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendMessage(Long senderId, ChatMessageCommand command) {
        // messageType 은 클라이언트가 보낸 값이다. SYSTEM(입장·퇴장)은 ChatRoomService 가 직접 생성하는
        // 서버 전용 타입이라, 클라이언트발 전송은 TEXT 만 허용한다(시스템 메시지 사칭 차단).
        if (command.messageType() != MessageType.TEXT) {
            throw new BaseException(ErrorCode.BAD_REQUEST);
        }

        UserSummary sender = userService.getUserSummary(senderId);

        Long sequence = chatMessageService.prepareSequence(command.chatRoomId(), senderId, command.messageType());

        chatMessageService.persistAndPublish(
                command.chatRoomId(), senderId, command.content(), command.messageType(), sequence, sender);

        chatMessageService.bufferAfterSend(command.chatRoomId(), senderId, sequence, command.messageType());
    }

    public void enterChatRoom(Long userId, Long chatRoomId) {
        // 시스템 메시지는 멤버 추가 시 자동 발송되므로 별도 처리 없음
    }

    public void leaveChatRoom(Long userId, Long chatRoomId) {
        UserSummary user = userService.getUserSummary(userId);
        ChatMessage savedMessage = chatRoomService.leaveChatRoom(chatRoomId, user);
        applicationEventPublisher.publishEvent(ChatMessageBroadcastEvent.from(savedMessage, user));
    }

    public void readChatRoom(Long userId, Long chatRoomId) {
        chatMessageService.markAsRead(chatRoomId, userId);
    }

    public void updateNotificationSetting(Long userId, Long roomId, boolean isOn) {
        chatRoomMemberService.updateNotificationSetting(roomId, userId, isOn);
    }

    public void updateChatRoomImage(Long userId, Long roomId, UploadFile uploadFile) {
        String imageUrl = null;

        if (uploadFile != null) {
            imageUrl = s3ImageUploader.upload(
                    uploadFile.originalFilename(),
                    uploadFile.contentType(),
                    uploadFile.inputStream(),
                    uploadFile.size());
        }

        chatRoomService.updateChatRoomImage(roomId, userId, imageUrl);
    }

    public void kickChatRoomMember(Long hostId, Long chatRoomId, Long targetUserId) {
        ChatMessage savedMessage = chatRoomService.kickChatRoomMember(chatRoomId, hostId, targetUserId);

        UserSummary targetUser = userService.getUserSummary(savedMessage.getSenderId());
        applicationEventPublisher.publishEvent(ChatMessageBroadcastEvent.from(savedMessage, targetUser));
    }

    /**
     * 게시글 ID에 해당하는 채팅방을 조회하거나 없으면 새로 생성하고 ID를 반환
     */
    public Long getOrCreateChatRoom(Long boardId) {
        return chatRoomService.getOrCreateChatRoom(boardId).getId();
    }

    public void addMember(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomService.getChatRoomOrThrow(chatRoomId);
        chatRoomMemberService.addMember(chatRoom, userId);
    }

    /**
     * 신규 입장 멤버에 대한 시스템 메시지를 생성하고 브로드캐스트 트리거 이벤트를 발행한다.
     * {@link com.back.catchmate.chat.event.ChatRoomMemberJoinedEvent} 리스너에서 호출.
     */
    public void welcomeNewMember(Long chatRoomId, Long userId) {
        UserSummary user = userService.getUserSummary(userId);
        ChatMessage joinMessage = chatRoomService.enterChatRoom(chatRoomId, user);
        applicationEventPublisher.publishEvent(ChatMessageBroadcastEvent.from(joinMessage, user));
    }

    /**
     * 게시글에 해당하는 채팅방을 보장하고 (없으면 생성) 멤버를 추가한 뒤 환영 시스템 메시지를 발행한다.
     * board.completed (작성자 입장) / enroll.accepted (신청자 입장) 양쪽에서 호출.
     */
    public void addBoardChatRoomMember(Long boardId, Long userId) {
        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(boardId);
        chatRoomMemberService.addMember(chatRoom, userId);
        welcomeNewMember(chatRoom.getId(), userId);
    }

    public void flushReadSequences() {
        chatMessageService.flushReadSequences();
    }

    public void flushRoomSequences() {
        chatMessageService.flushRoomSequences();
    }
}
