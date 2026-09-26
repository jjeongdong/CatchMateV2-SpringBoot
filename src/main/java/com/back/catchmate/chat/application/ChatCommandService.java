package com.back.catchmate.chat.application;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.chat.application.dto.command.ChatMessageSendCommand;
import com.back.catchmate.chat.domain.ChatFocusRoomStore;
import com.back.catchmate.chat.domain.ChatHistoryCache;
import com.back.catchmate.chat.domain.ChatMembershipCache;
import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomImageUploader;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.domain.ChatRoomSequenceBuffer;
import com.back.catchmate.chat.domain.ChatSequenceStore;
import com.back.catchmate.chat.domain.ReadSequence;
import com.back.catchmate.chat.domain.ReadSequenceBuffer;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.domain.exception.ChatRoomMemberNotFoundException;
import com.back.catchmate.chat.domain.exception.ChatSenderUnauthenticatedException;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCommandService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSequenceStore chatSequenceStore;
    private final ChatRoomSequenceBuffer chatRoomSequenceBuffer;
    private final ReadSequenceBuffer readSequenceBuffer;
    private final ChatMembershipCache chatMembershipCache;
    private final ChatHistoryCache chatHistoryCache;
    private final ChatFocusRoomStore chatFocusRoomStore;
    private final ChatRoomImageUploader chatRoomImageUploader;
    private final ChatMembershipReader chatMembershipReader;
    private final ChatReadRecorder chatReadRecorder;
    private final ChatMessageWriter chatMessageWriter;
    private final ChatBufferFlushExecutor chatBufferFlushExecutor;
    private final UserQueryApi userQueryApi;
    private final BoardQueryApi boardQueryApi;
    private final ApplicationEventPublisher eventPublisher;

    // DB 커넥션을 메시지 INSERT(+Outbox 저장)에만 잡히게 하려고 트랜잭션을 걸지 않는다.
    // 멤버십(캐시)·시퀀스는 Redis 로 트랜잭션 밖에서, INSERT + 이벤트 발행만 ChatMessageWriter 의 좁은 트랜잭션에서 한다.
    public void sendMessage(Long senderId, ChatMessageSendCommand command) {
        requireSender(senderId);
        command.messageType().verifySendableByUser();
        UserInfo sender = userQueryApi.getInfo(senderId);
        // 시퀀스보다 먼저 확인해야 비멤버·읽기 전용이 방 시퀀스를 올리지 못한다.
        chatMembershipReader.get(command.chatRoomId(), senderId).verifySendable();
        Long sequence = chatSequenceStore.next(command.chatRoomId());
        chatMessageWriter.writeText(command.chatRoomId(), sender, command.content(), sequence);
        bufferAfterSend(command.chatRoomId(), senderId, sequence);
    }

    @Transactional
    public void leaveChatRoom(Long userId, Long chatRoomId) {
        requireSender(userId);
        UserInfo user = userQueryApi.getInfo(userId);
        Long sequence = chatSequenceStore.current(chatRoomId);
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(ChatRoomMemberNotFoundException::new);
        member.leave(LocalDateTime.now());
        chatMembershipCache.evict(chatRoomId, userId);
        saveSystemMessage(
                ChatMessage.left(chatRoomRepository.getReference(chatRoomId), userId, user.nickName(), sequence), user);
    }

    @Transactional
    public void kickChatRoomMember(Long hostId, Long chatRoomId, Long targetUserId) {
        ChatRoom chatRoom = chatRoomRepository.getById(chatRoomId);
        Long sequence = chatSequenceStore.current(chatRoomId);
        chatRoom.verifyKick(hostId, boardQueryApi.getInfo(chatRoom.getBoardId()).userId(), targetUserId);
        ChatRoomMember target = findActiveMember(chatRoomId, targetUserId);
        target.leave(LocalDateTime.now());
        // 강퇴된 사용자가 캐시 TTL 동안 계속 전송하지 못하게 한다.
        chatMembershipCache.evict(chatRoomId, targetUserId);
        UserInfo targetUser = userQueryApi.getInfo(targetUserId);
        saveSystemMessage(ChatMessage.kicked(chatRoom, targetUserId, targetUser.nickName(), sequence), targetUser);
    }

    @Transactional
    public void updateNotificationSetting(Long userId, Long chatRoomId, boolean notificationOn) {
        ChatRoomMember member = findActiveMember(chatRoomId, userId);
        if (notificationOn) {
            member.enableNotification();
        } else {
            member.disableNotification();
        }
        chatMembershipCache.evict(chatRoomId, userId);
    }

    @Transactional
    public void updateChatRoomImage(Long userId, Long chatRoomId, UploadFile file) {
        chatMembershipReader.get(chatRoomId, userId).verifyActive();
        ChatRoom chatRoom = chatRoomRepository.getById(chatRoomId);
        chatRoom.changeImage(file != null ? chatRoomImageUploader.upload(file) : null);
    }

    public void readChatRoom(Long userId, Long chatRoomId) {
        requireSender(userId);
        chatReadRecorder.record(chatRoomId, userId);
    }

    public void focusChatRoom(Long userId, Long chatRoomId) {
        chatFocusRoomStore.focus(userId, chatRoomId);
    }

    public void unfocusChatRoom(Long userId) {
        chatFocusRoomStore.unfocus(userId);
    }

    /**
     * 게시글 발행(작성자)·신청 수락(신청자) 리스너 전용. 호출자의 트랜잭션에 참여해, 입장 실패(재입장 금지 등)가
     * 원래 작업도 취소하게 한다 (spec Q3). 이미 활성 멤버여도 옛 동작처럼 입장 메시지를 남긴다.
     */
    @Transactional
    public void addBoardChatRoomMember(Long boardId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository
                .findByBoardId(boardId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.create(boardId)));
        chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), userId)
                .ifPresentOrElse(
                        ChatRoomMember::rejoin,
                        () -> chatRoomMemberRepository.save(ChatRoomMember.create(
                                chatRoom, userId, chatRoom.getLastMessageSequence(), LocalDateTime.now())));
        chatMembershipCache.evict(chatRoom.getId(), userId);
        UserInfo user = userQueryApi.getInfo(userId);
        saveSystemMessage(
                ChatMessage.joined(chatRoom, userId, user.nickName(), chatSequenceStore.current(chatRoom.getId())),
                user);
    }

    public void flushReadSequences() {
        List<ReadSequence> readSequences = readSequenceBuffer.drainAll();
        if (readSequences.isEmpty()) {
            return;
        }
        try {
            chatBufferFlushExecutor.flushReadSequences(readSequences);
        } catch (Exception e) {
            // DB 반영 실패(Hikari 타임아웃·데드락·DB 장애 등) → 이미 drain(DEL)된 읽음 시퀀스를 버퍼로 되돌려 다음 tick 이 재시도한다.
            // buffer()는 (room,user)별 monotonic HSET 이라, 되돌리는 사이 유저가 더 읽었으면 그 큰 값이 유지된다.
            // UPDATE 도 monotonic guard(last_read_sequence < :sequence) 라 재적용은 멱등하다.
            readSequences.forEach(read -> readSequenceBuffer.buffer(read.chatRoomId(), read.userId(), read.sequence()));
            log.error("읽음 시퀀스 DB 반영 실패, 버퍼로 되돌림 count={}", readSequences.size(), e);
        }
    }

    public void flushRoomSequences() {
        Map<Long, Long> sequenceByChatRoomId = chatRoomSequenceBuffer.drainAll();
        if (sequenceByChatRoomId.isEmpty()) {
            return;
        }
        try {
            chatBufferFlushExecutor.flushRoomSequences(sequenceByChatRoomId);
        } catch (Exception e) {
            // 읽음 시퀀스와 같은 이유로 버퍼로 되돌린다 (방별 monotonic HSET + monotonic UPDATE guard).
            sequenceByChatRoomId.forEach(chatRoomSequenceBuffer::buffer);
            log.error("채팅방 시퀀스 DB 반영 실패, 버퍼로 되돌림 count={}", sequenceByChatRoomId.size(), e);
        }
    }

    // 커밋 후라 메시지는 이미 확정이다. 예외를 올리면 발신자가 "재전송 유효" 로 통보받아 같은 메시지를
    // 중복 저장하게 되므로 삼키고 로그만 남긴다. 버퍼는 monotonic 이라 다음 메시지나 스케줄러가 자가치유한다.
    private void bufferAfterSend(Long chatRoomId, Long senderId, Long sequence) {
        try {
            chatRoomSequenceBuffer.buffer(chatRoomId, sequence);
            readSequenceBuffer.buffer(chatRoomId, senderId, sequence);
            chatHistoryCache.evictLatestPage(chatRoomId);
        } catch (Exception e) {
            log.error("메시지 전송 후처리 실패 chatRoomId={}, senderId={}, sequence={}", chatRoomId, senderId, sequence, e);
        }
    }

    // STOMP 는 @AuthUser 가 없어 Principal 이 비거나 형식이 틀리면 userId 가 null 로 온다 (ChatStompController).
    // 표현 계층은 도메인 예외를 던질 수 없어 여기서 확인한다. REST 경로에서는 항상 통과한다.
    private static void requireSender(Long userId) {
        if (userId == null) {
            throw new ChatSenderUnauthenticatedException();
        }
    }

    private ChatRoomMember findActiveMember(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, userId)
                .filter(ChatRoomMember::isActive)
                .orElseThrow(ChatRoomMemberNotFoundException::new);
    }

    private void saveSystemMessage(ChatMessage message, UserInfo user) {
        ChatMessage saved = chatMessageRepository.save(message);
        chatHistoryCache.evictLatestPage(saved.getChatRoom().getId());
        eventPublisher.publishEvent(ChatMessageBroadcastEvent.of(saved, user.nickName(), user.profileImageUrl()));
    }
}
