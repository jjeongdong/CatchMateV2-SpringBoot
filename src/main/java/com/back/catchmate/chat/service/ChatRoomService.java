package com.back.catchmate.chat.service;
import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.chat.entity.ChatMessage;
import com.back.catchmate.chat.entity.ChatRoom;
import com.back.catchmate.chat.entity.ChatRoomMember;
import com.back.catchmate.chat.entity.MessageType;
import com.back.catchmate.chat.dto.MembershipSnapshot;
import com.back.catchmate.chat.infra.ChatHistoryRedisCache;
import com.back.catchmate.chat.infra.ChatMembershipRedisCache;
import com.back.catchmate.chat.infra.ChatSequenceRedisStore;
import com.back.catchmate.chat.repository.ChatMessageRepository;
import com.back.catchmate.chat.repository.ChatRoomMemberRepository;
import com.back.catchmate.chat.repository.ChatRoomRepository;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final ChatHistoryRedisCache chatHistoryRedisCache;
    private final ChatMembershipRedisCache chatMembershipRedisCache;
    private final ChatSequenceRedisStore chatSequenceRedisStore;
    private final BoardService boardService;
    private final UserService userService;

    public ChatRoom getChatRoomOrThrow(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_NOT_FOUND));
    }

    public Optional<ChatRoom> findById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId);
    }

    public Optional<ChatRoom> findByBoardId(Long boardId) {
        return chatRoomRepository.findByBoardId(boardId);
    }

    @Transactional
    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public ChatRoom getOrCreateChatRoom(Long boardId) {
        return chatRoomRepository.findByBoardId(boardId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.createChatRoom(boardId)));
    }

    public Page<ChatRoom> findAllByUserId(Long userId, Pageable pageable) {
        return chatRoomRepository.findAllByUserIdWithPaging(userId, pageable);
    }

    public List<ChatRoom> findAllByUserId(Long userId) {
        return chatRoomRepository.findAllByUserId(userId);
    }

    @Transactional
    public ChatMessage enterChatRoom(Long chatRoomId, UserSummary user) {
        Long sequence = chatSequenceRedisStore.getCurrentSequence(chatRoomId);

        String enterMessage = user.nickName() + "님이 입장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoomId,
                user.userId(),
                enterMessage,
                MessageType.SYSTEM,
                sequence
        );

        chatMessage = chatMessageRepository.save(chatMessage);
        chatHistoryRedisCache.evictLatestPage(chatRoomId);
        return chatMessage;
    }

    @Transactional
    public ChatMessage leaveChatRoom(Long chatRoomId, UserSummary user) {
        Long sequence = chatSequenceRedisStore.getCurrentSequence(chatRoomId);

        ChatRoomMember chatRoomMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, user.userId())
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        chatRoomMember.leave();
        chatRoomMemberRepository.save(chatRoomMember);
        // 멤버십 상태 변경 → 인증 캐시 무효화 (choke point 는 ChatRoomMemberService.saveMember 이지만
        // 이 경로는 멤버 리포지토리를 직접 쓰므로 여기서 명시적으로 evict 한다)
        chatMembershipRedisCache.evict(chatRoomId, chatRoomMember.getUserId());

        String leaveMessage = user.nickName() + "님이 퇴장하셨습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoomId,
                user.userId(),
                leaveMessage,
                MessageType.SYSTEM,
                sequence
        );

        chatMessage = chatMessageRepository.save(chatMessage);
        chatHistoryRedisCache.evictLatestPage(chatRoomId);
        return chatMessage;
    }

    public boolean validateUserInChatRoom(Long userId, Long roomId) {
        if (!isActiveMember(roomId, userId)) {
            throw new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND);
        }

        return true;
    }

    // 멤버십 인증 캐시(read-through). miss 시에만 DB 조회 후 캐시 적재.
    private boolean isActiveMember(Long roomId, Long userId) {
        return chatMembershipRedisCache.find(roomId, userId)
                .map(MembershipSnapshot::active)
                .orElseGet(() -> {
                    Optional<ChatRoomMember> member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId);
                    if (member.isEmpty()) {
                        return false;
                    }
                    ChatRoomMember found = member.get();
                    chatMembershipRedisCache.put(roomId, userId,
                            new MembershipSnapshot(found.isActive(), found.isReadOnly()));
                    return found.isActive();
                });
    }

    @Transactional
    public void updateChatRoomImage(Long chatRoomId, Long userId, String imageUrl) {
        validateUserInChatRoom(userId, chatRoomId);

        ChatRoom chatRoom = getChatRoomOrThrow(chatRoomId);
        chatRoom.updateImageUrl(imageUrl);

        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public ChatMessage kickChatRoomMember(Long chatRoomId, Long hostId, Long targetUserId) {
        ChatRoom chatRoom = getChatRoomOrThrow(chatRoomId);
        Long sequence = chatSequenceRedisStore.getCurrentSequence(chatRoomId);

        BoardSummary board = boardService.getBoardSummary(chatRoom.getBoardId());
        if (!board.userId().equals(hostId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (hostId.equals(targetUserId)) {
            throw new BaseException(ErrorCode.BAD_REQUEST);
        }

        ChatRoomMember targetMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        targetMember.leave();
        chatRoomMemberRepository.save(targetMember);
        // 멤버십 상태 변경 → 인증 캐시 무효화 (강퇴 유저가 TTL 동안 계속 전송하는 것을 막는다)
        chatMembershipRedisCache.evict(chatRoomId, targetMember.getUserId());

        UserSummary targetUser = userService.getUserSummary(targetMember.getUserId());
        String kickMessage = targetUser.nickName() + "님이 내보내졌습니다.";
        ChatMessage chatMessage = ChatMessage.createMessage(
                chatRoomId,
                targetUser.userId(),
                kickMessage,
                MessageType.SYSTEM,
                sequence
        );

        chatMessage = chatMessageRepository.save(chatMessage);
        chatHistoryRedisCache.evictLatestPage(chatRoomId);
        return chatMessage;
    }
}
