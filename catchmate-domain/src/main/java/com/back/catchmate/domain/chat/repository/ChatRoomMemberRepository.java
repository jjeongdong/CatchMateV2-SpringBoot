package com.back.catchmate.domain.chat.repository;

import com.back.catchmate.domain.chat.model.ChatRoomMember;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository {
    ChatRoomMember save(ChatRoomMember member);

    Optional<ChatRoomMember> findById(Long id);

    /**
     * 특정 채팅방과 사용자로 멤버 조회
     */
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 사용자가 참가한 모든 채팅방 멤버 목록 조회 (활성 상태만)
     */
    List<ChatRoomMember> findAllByUserIdAndActive(Long userId);

    /**
     * 특정 채팅방의 모든 활성 멤버 조회
     */
    List<ChatRoomMember> findAllByChatRoomIdAndActive(Long chatRoomId);

    /**
     * 사용자가 특정 채팅방의 활성 멤버인지 확인
     */
    boolean existsByChatRoomIdAndUserIdAndActive(Long chatRoomId, Long userId);

    void delete(ChatRoomMember member);
}
