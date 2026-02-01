package com.back.catchmate.application.chat;

import com.back.catchmate.application.chat.dto.command.ChatMessageCommand;
import com.back.catchmate.application.chat.dto.response.ChatMessageResponse;
import com.back.catchmate.application.chat.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.application.chat.dto.response.ChatRoomResponse;
import com.back.catchmate.application.chat.event.ChatNotificationEvent;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.domain.chat.model.MessageType;
import com.back.catchmate.domain.chat.service.ChatMessageService;
import com.back.catchmate.domain.chat.service.ChatRoomMemberService;
import com.back.catchmate.domain.chat.service.ChatRoomService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatUseCase {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

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

        // 발신자를 제외한 채팅방 멤버들에게 FCM 알림 이벤트 발행
        publishChatNotificationEvent(savedMessage, command.getSenderId());

        return ChatMessageResponse.from(savedMessage);
    }

    /**
     * 채팅 알림 이벤트 발행
     * 발신자를 제외한 채팅방 멤버들에게 FCM Push 알림
     */
    private void publishChatNotificationEvent(ChatMessage savedMessage, Long senderId) {
        // 채팅방의 활성 멤버 조회 (발신자 제외)
        List<User> recipients = chatRoomMemberService
                .getActiveMembersByChatRoomId(savedMessage.getChatRoom().getId())
                .stream()
                .map(ChatRoomMember::getUser)
                .filter(user -> !user.getId().equals(senderId))  // 발신자 제외
                .toList();

        if (!recipients.isEmpty()) {
            eventPublisher.publishEvent(ChatNotificationEvent.of(savedMessage, recipients));
        }
    }

    /**
     * 채팅방의 모든 메시지 조회 (페이징)
     */
    public DomainPage<ChatMessage> getMessages(Long chatRoomId, DomainPageable pageable) {
        return chatMessageService.findAllByChatRoomId(chatRoomId, pageable);
    }

    /**
     * 내가 속한 채팅방 목록 조회 (페이징)
     * 각 채팅방의 마지막 메시지도 함께 조회
     */
    public PagedResponse<ChatRoomResponse> getMyChatRooms(Long userId, DomainPageable pageable) {
        DomainPage<ChatRoom> chatRoomPage = chatRoomService.findAllByUserId(userId, pageable);

        // 각 채팅방의 마지막 메시지를 조회하여 ChatRoomResponse로 변환
        List<ChatRoomResponse> responses = chatRoomPage.getContent().stream()
                .map(chatRoom -> {
                    ChatMessageResponse lastMessage = chatMessageService.findLastMessageByChatRoomId(chatRoom.getId())
                            .map(ChatMessageResponse::from)
                            .orElse(null);
                    return ChatRoomResponse.from(chatRoom, lastMessage);
                })
                .toList();

        return new PagedResponse<>(chatRoomPage, responses);
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
     * 서버가 퇴장 메시지를 자동으로 생성하고, ChatRoomMember에서 퇴장 처리
     */
    @Transactional
    public ChatMessageResponse leaveChatRoom(Long userId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomService.getChatRoom(chatRoomId);
        User user = userService.getUser(userId);

        // 채팅방 멤버에서 퇴장 처리
        chatRoomMemberService.removeMember(chatRoomId, userId);

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

    /**
     * 특정 채팅방의 참여자 목록 조회
     * @param chatRoomId 채팅방 ID
     * @return 채팅방에 현재 참여중인 사용자 목록
     */
    public List<ChatRoomMemberResponse> getChatRoomMembers(Long chatRoomId) {
        // 채팅방 존재 여부 확인
        chatRoomService.getChatRoom(chatRoomId);

        // 활성 멤버 목록 조회
        List<ChatRoomMember> activeMembers = chatRoomMemberService.getActiveMembersByChatRoomId(chatRoomId);

        return activeMembers.stream()
                .map(ChatRoomMemberResponse::from)
                .toList();
    }
}
