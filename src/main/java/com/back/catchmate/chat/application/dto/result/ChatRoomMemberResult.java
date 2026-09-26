package com.back.catchmate.chat.application.dto.result;

import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record ChatRoomMemberResult(
        Long memberId, Long userId, String nickName, String profileImageUrl, LocalDateTime joinedAt) {

    // 탈퇴한 사용자는 닉네임·프로필을 비운다 (옛 코드는 NPE).
    public static ChatRoomMemberResult of(ChatRoomMember member, UserInfo user) {
        return new ChatRoomMemberResult(
                member.getId(),
                member.getUserId(),
                user != null ? user.nickName() : null,
                user != null ? user.profileImageUrl() : null,
                member.getJoinedAt());
    }
}
