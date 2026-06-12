package com.back.catchmate.chat.application.service;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.chat.application.dto.ChatMessageListDto;
import com.back.catchmate.chat.application.dto.command.ChatMessageCommand;
import com.back.catchmate.chat.application.dto.response.ChatMessageResponse;
import com.back.catchmate.chat.application.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.chat.application.dto.response.ChatRoomResponse;
import com.back.catchmate.chat.application.event.ChatMessageEvent;
import com.back.catchmate.chat.application.event.ChatNotificationEvent;
import com.back.catchmate.chat.application.port.in.ChatUseCase;
import com.back.catchmate.chat.application.port.out.BoardFetchPort;
import com.back.catchmate.chat.application.port.out.UserFetchPort;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.user.application.dto.command.UploadFile;
import com.back.catchmate.user.application.port.out.ImageUploaderPort;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService implements ChatUseCase {

    private final ImageUploaderPort imageUploaderPort;

    private final ChatMessageService chatMessageService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final ChatRoomService chatRoomService;

    private final BoardFetchPort boardFetchPort;
    private final UserFetchPort userFetchPort;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void sendMessage(Long senderId, ChatMessageCommand command) {
        User sender = userFetchPort.getUser(senderId);

        ChatMessage savedMessage = chatMessageService.saveMessage(
                command.chatRoomId(),
                senderId,
                command.content(),
                command.messageType()
        );

        // 웹 소켓을 통해 채팅 메시지 이벤트 발행 (채팅방 멤버 전체에게 실시간 전송)
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(savedMessage, sender));

        // 채팅방 멤버 중 발신자를 제외한 모든 사용자에게 알림 이벤트 발행
        // FCM 알림은 별도의 이벤트 리스너에서 처리
        List<Long> recipientIds = chatRoomMemberService.getChatRoomMembers(savedMessage.getChatRoomId())
                .stream()
                .filter(member -> !member.getUserId().equals(senderId))
                .filter(ChatRoomMember::isNotificationOn)
                .map(ChatRoomMember::getUserId)
                .toList();
        List<User> recipients = recipientIds.isEmpty() ? List.of() : userFetchPort.getUsers(recipientIds);

        // 알림을 받을 사용자가 있을 때만 이벤트 발행
        if (!recipients.isEmpty()) {
            // 채팅 메시지 알림 이벤트 발행 (FCM 또는 웹 소켓을 통해 사용자에게 전송)
            applicationEventPublisher.publishEvent(ChatNotificationEvent.of(savedMessage, sender, recipients));
        }
    }

    @Transactional
    public void enterChatRoom(Long userId, Long chatRoomId) {
        // 기존의 enterChatRoom 로직을 비워둠 (시스템 메시지는 멤버 추가 시 자동 발송되므로)
        // 만약 '채팅방 입장' 시점에 필요한 다른 처리가 있다면 여기에 추가
    }

    @Transactional
    public void leaveChatRoom(Long userId, Long chatRoomId) {
        User user = userFetchPort.getUser(userId);
        ChatMessage savedMessage = chatRoomService.leaveChatRoom(chatRoomId, user);
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(savedMessage, user));
    }

    public PagedResponse<ChatRoomResponse> getMyChatRooms(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> chatRoomPage = chatRoomService.findAllByUserId(userId, pageable);

        List<Long> chatRoomIds = chatRoomPage.getContent().stream()
                .map(ChatRoom::getId)
                .toList();

        Map<Long, ChatMessage> lastMessageMap = chatMessageService.getLastMessagesByChatRoomIds(chatRoomIds);
        Map<Long, ChatRoomMember> memberMap = chatRoomMemberService.getChatRoomMembersByChatRoomIds(chatRoomIds, userId);

        List<Long> boardIds = chatRoomPage.getContent().stream()
                .map(ChatRoom::getBoardId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<Board> boards = boardIds.isEmpty() ? List.of() : boardFetchPort.getBoards(boardIds);
        Map<Long, BoardResponse> boardResponseById = boardFetchPort.buildBoardResponses(boards, id -> false).stream()
                .collect(Collectors.toMap(BoardResponse::boardId, Function.identity()));

        Map<Long, User> lastMessageSenderById = resolveSenders(List.copyOf(lastMessageMap.values()));

        List<ChatRoomResponse> responses = chatRoomPage.getContent().stream()
                .map(chatRoom -> {
                    ChatMessageResponse lastMessage = Optional.ofNullable(lastMessageMap.get(chatRoom.getId()))
                            .map(msg -> ChatMessageResponse.from(msg, lastMessageSenderById.get(msg.getSenderId())))
                            .orElse(null);

                    ChatRoomMember member = memberMap.get(chatRoom.getId());

                    long unreadCount = member != null
                            ? member.calculateUnreadCount(chatRoom.getLastMessageSequence())
                            : 0;
                    boolean isNotificationOn = member != null && member.isNotificationOn();
                    boolean readOnly = member != null && member.isReadOnly();

                    BoardResponse boardResponse = chatRoom.getBoardId() != null ? boardResponseById.get(chatRoom.getBoardId()) : null;
                    return ChatRoomResponse.from(chatRoom, boardResponse, lastMessage, unreadCount, isNotificationOn, readOnly);
                })
                .toList();

        return new PagedResponse<>(chatRoomPage, responses);
    }

    public void readChatRoom(Long userId, Long chatRoomId) {
        chatMessageService.markAsRead(chatRoomId, userId);
    }

    public List<ChatMessageResponse> getChatHistory(Long userId, Long roomId, Long lastMessageId, int size) {
        chatRoomService.validateUserInChatRoom(userId, roomId);

        ChatMessageListDto cacheDtoList = chatMessageService.getChatHistory(roomId, lastMessageId, size);

        return cacheDtoList.getMessages().stream()
                .map(dto -> new ChatMessageResponse(
                dto.getId(),
                dto.getRoomId(),
                dto.getSenderId(),
                dto.getSenderNickname(),
                dto.getSenderProfileImageUrl(),
                dto.getContent(),
                MessageType.valueOf(dto.getMessageType().name()),
                dto.getCreatedAt()
        )
                )
                .toList();
    }

    public List<ChatMessageResponse> syncMessages(Long userId, Long roomId, Long lastMessageId, int size) {
        // 1. 해당 채팅방의 멤버인지 권한 검증
        chatRoomService.validateUserInChatRoom(userId, roomId);

        // 2. 누락된 동기화 메시지 조회
        List<ChatMessage> syncMessages = chatMessageService.getSyncMessages(roomId, lastMessageId, size);

        // 3. 응답 DTO로 변환하여 반환
        Map<Long, User> senderById = resolveSenders(syncMessages);
        return syncMessages.stream()
                .map(msg -> ChatMessageResponse.from(msg, senderById.get(msg.getSenderId())))
                .toList();
    }

    public ChatMessageResponse getLastMessage(Long chatRoomId) {
        return chatMessageService.getLastMessage(chatRoomId)
                .map(msg -> ChatMessageResponse.from(msg, userFetchPort.getUser(msg.getSenderId())))
                .orElse(null);
    }

    private Map<Long, User> resolveSenders(List<ChatMessage> messages) {
        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .toList();
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return userFetchPort.getUsers(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public boolean canAccessChatRoom(Long userId, Long chatRoomId) {
        return chatRoomService.validateUserInChatRoom(userId, chatRoomId);
    }

    public List<ChatRoomMemberResponse> getChatRoomMembers(Long chatRoomId) {
        List<ChatRoomMember> activeMembers = chatRoomMemberService.getChatRoomMembers(chatRoomId);

        List<Long> userIds = activeMembers.stream()
                .map(ChatRoomMember::getUserId)
                .distinct()
                .toList();
        Map<Long, User> userById = userFetchPort.getUsers(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return activeMembers.stream()
                .map(member -> ChatRoomMemberResponse.from(member, userById.get(member.getUserId())))
                .toList();
    }

    @Transactional
    public void updateNotificationSetting(Long userId, Long roomId, boolean isOn) {
        chatRoomMemberService.updateNotificationSetting(roomId, userId, isOn);
    }

    @Transactional
    public void updateChatRoomImage(Long userId, Long roomId, UploadFile uploadFile) {
        String imageUrl = null;

        // 1. 이미지가 전달된 경우 S3 등에 업로드하여 URL 획득
        if (uploadFile != null) {
            imageUrl = imageUploaderPort.upload(
                    uploadFile.originalFilename(),
                    uploadFile.contentType(),
                    uploadFile.inputStream(),
                    uploadFile.size()
            );
        }

        // 2. ChatRoomService를 호출하여 획득한 URL을 DB에 반영
        chatRoomService.updateChatRoomImage(roomId, userId, imageUrl);
    }

    @Transactional
    public void flushReadSequences() {
        chatMessageService.flushReadSequences();
    }

    @Transactional
    public void flushMessages() {
        chatMessageService.flushMessages();
    }

    @Transactional
    public void kickChatRoomMember(Long hostId, Long chatRoomId, Long targetUserId) {
        ChatMessage savedMessage = chatRoomService.kickChatRoomMember(chatRoomId, hostId, targetUserId);

        // 2. 방에 남아있는 사람들에게 강퇴 시스템 메시지를 소켓으로 실시간 전송
        User targetUser = userFetchPort.getUser(savedMessage.getSenderId());
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(savedMessage, targetUser));
    }
}
