package com.back.catchmate.application.chat.service;

import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.domain.chat.repository.ChatRoomMemberRepository;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 채팅방 멤버 추가
     */
    public void addMember(ChatRoom chatRoom, User user) {
        // 이미 멤버인지 확인
        Optional<ChatRoomMember> existing = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), user.getId());

        if (existing.isPresent()) {
            ChatRoomMember member = existing.get();
            // 이미 활성 멤버면 그대로 반환
            if (member.isActive()) {
                return;
            }

            // 비활성 멤버면 재입장 허용 여부에 따라 처리
            throw new BaseException(ErrorCode.CHATROOM_REENTRY_NOT_ALLOWED);
        }

        ChatRoomMember newMember = ChatRoomMember.create(chatRoom, user);
        chatRoomMemberRepository.save(newMember);
    }

    /**
     * 채팅방 멤버 퇴장
     */
    public void removeMember(Long chatRoomId, Long userId) {
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));

        member.leave();
        chatRoomMemberRepository.save(member);
    }

    /**
     * 사용자가 참가한 채팅방 ID 목록 조회 (활성 상태만)
     */
    public List<Long> getChatRoomIdsByUserId(Long userId) {
        return chatRoomMemberRepository.findAllByUserIdAndActive(userId).stream()
                .map(member -> member.getChatRoom().getId())
                .toList();
    }

    /**
     * 특정 채팅방의 활성 멤버 목록 조회
     */
    public List<ChatRoomMember> getActiveMembersByChatRoomId(Long chatRoomId) {
        return chatRoomMemberRepository.findAllByChatRoomIdAndActive(chatRoomId);
    }

    /**
     * 사용자가 특정 채팅방의 활성 멤버인지 확인
     */
    public boolean isActiveMember(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndActive(chatRoomId, userId);
    }
}
