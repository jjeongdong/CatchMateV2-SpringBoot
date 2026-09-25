package com.back.catchmate.chat.dto.response;

import com.back.catchmate.chat.entity.ChatRoomMember;
import com.back.catchmate.user.dto.response.UserSummary;
import java.time.LocalDateTime;

public record ChatRoomMemberResponse(
        Long memberId, Long userId, String nickName, String profileImageUrl, LocalDateTime joinedAt) {
    public static ChatRoomMemberResponse from(ChatRoomMember member, UserSummary user) {
        return new ChatRoomMemberResponse(
                member.getId(), user.userId(), user.nickName(), user.profileImageUrl(), member.getJoinedAt());
    }
}
