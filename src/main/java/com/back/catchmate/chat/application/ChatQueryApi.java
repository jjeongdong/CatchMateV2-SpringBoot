package com.back.catchmate.chat.application;

import com.back.catchmate.chat.application.dto.api.ChatRecipientInfo;
import com.back.catchmate.chat.domain.ChatFocusRoomStore;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatQueryApi {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatFocusRoomStore chatFocusRoomStore;

    /**
     * 게시글에 딸린 채팅방 ID 를 찾는다.
     *
     * @param boardId 게시글 ID
     * @return 채팅방 ID, 채팅방이 없으면 empty
     */
    @Transactional(readOnly = true)
    public Optional<Long> findChatRoomIdByBoardId(Long boardId) {
        return chatRoomRepository.findByBoardId(boardId).map(ChatRoom::getId);
    }

    /**
     * 채팅 알림을 받을 후보를 구한다 (발신자를 뺀 활성 멤버와 그 방의 알림 설정).
     *
     * @param chatRoomId    채팅방 ID
     * @param excludeUserId 제외할 사용자 (발신자)
     * @return 수신 후보 목록, 없으면 빈 목록
     */
    @Transactional(readOnly = true)
    public List<ChatRecipientInfo> getRecipients(Long chatRoomId, Long excludeUserId) {
        return chatRoomMemberRepository.findActiveByChatRoomId(chatRoomId).stream()
                .filter(member -> !member.getUserId().equals(excludeUserId))
                .map(member -> new ChatRecipientInfo(member.getUserId(), member.isNotificationOn()))
                .toList();
    }

    /**
     * 사용자가 지금 보고 있는 채팅방을 찾는다 (그 방의 푸시 억제용). Redis 전용이라 트랜잭션을 걸지 않는다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 ID, 보고 있는 방이 없거나 Redis 장애면 empty
     */
    public Optional<Long> findFocusRoom(Long userId) {
        return chatFocusRoomStore.find(userId);
    }

    /**
     * 여러 사용자가 지금 보고 있는 채팅방을 한 번에 찾는다.
     *
     * @param userIds 사용자 ID 들
     * @return 사용자 ID → 채팅방 ID. 보고 있는 방이 있는 사용자만 담긴다. Redis 장애면 빈 맵
     */
    public Map<Long, Long> getFocusRooms(List<Long> userIds) {
        return chatFocusRoomStore.findAll(userIds);
    }
}
