package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.port.out.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 채팅방 멤버 추가
     */
    public ChatRoomMember addMember(ChatRoom chatRoom, Long userId) {
        // 이미 멤버인지 확인
        Optional<ChatRoomMember> existing = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), userId);

        if (existing.isPresent()) {
            ChatRoomMember member = existing.get();
            // 이미 활성 멤버면 — read-only 였다면 새 매칭이므로 해제
            if (member.isActive()) {
                if (member.isReadOnly()) {
                    member.clearReadOnly();
                    chatRoomMemberRepository.save(member);
                }
                return member;
            }

            // 비활성 멤버면 재입장 허용 여부에 따라 처리
            throw new BaseException(ErrorCode.CHATROOM_REENTRY_NOT_ALLOWED);
        }

        ChatRoomMember newMember = ChatRoomMember.create(chatRoom.getId(), userId, chatRoom.getLastMessageSequence());
        return chatRoomMemberRepository.save(newMember);
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
                .map(ChatRoomMember::getChatRoomId)
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

    /**
     * 특정 채팅방의 활성 멤버 단건 조회 (없으면 예외)
     */
    public ChatRoomMember getChatRoomMember(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .filter(ChatRoomMember::isActive)
                .orElseThrow(() -> new BaseException(ErrorCode.CHATROOM_MEMBER_NOT_FOUND));
    }

    /**
     * 특정 채팅방의 활성 멤버 목록 조회 (getActiveMembersByChatRoomId의 별칭)
     */
    public List<ChatRoomMember> getChatRoomMembers(Long chatRoomId) {
        return chatRoomMemberRepository.findAllByChatRoomIdAndActive(chatRoomId);
    }

    /**
     * 여러 채팅방의 특정 사용자 멤버 정보 일괄 조회
     */
    public Map<Long, ChatRoomMember> getChatRoomMembersByChatRoomIds(List<Long> chatRoomIds, Long userId) {
        return chatRoomMemberRepository.findByChatRoomIdsAndUserId(chatRoomIds, userId);
    }

    /**
     * 멤버별 알림 수신 설정 변경
     */
    public void updateNotificationSetting(Long chatRoomId, Long userId, boolean isOn) {
        ChatRoomMember member = getChatRoomMember(chatRoomId, userId);
        member.updateNotificationSetting(isOn);
        chatRoomMemberRepository.save(member);
    }
}
