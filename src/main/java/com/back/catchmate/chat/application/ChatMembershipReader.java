package com.back.catchmate.chat.application;

import com.back.catchmate.chat.domain.ChatMembershipCache;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.MembershipSnapshot;
import com.back.catchmate.chat.domain.exception.ChatRoomMemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 메시지 전송·구독·조회마다 반복되는 멤버십 확인을 Redis 로 흡수한다 (read-through).
// 트랜잭션을 걸지 않는다 — 전송 경로가 DB 커넥션을 INSERT 에만 잡도록 하려는 설계라서다.
@Service
@RequiredArgsConstructor
public class ChatMembershipReader {
    private final ChatMembershipCache chatMembershipCache;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // 멤버 행이 없으면 ChatRoomMemberNotFoundException. 퇴장·읽기 전용 판단은 스냅샷이 한다.
    public MembershipSnapshot get(Long chatRoomId, Long userId) {
        return chatMembershipCache.find(chatRoomId, userId).orElseGet(() -> {
            ChatRoomMember member = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoomId, userId)
                    .orElseThrow(ChatRoomMemberNotFoundException::new);
            MembershipSnapshot snapshot = member.snapshot();
            chatMembershipCache.put(chatRoomId, userId, snapshot);
            return snapshot;
        });
    }
}
