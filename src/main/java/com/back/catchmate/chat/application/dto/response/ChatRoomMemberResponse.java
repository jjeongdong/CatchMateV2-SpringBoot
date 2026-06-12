package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.user.domain.model.User;

import java.time.LocalDateTime;

public record ChatRoomMemberResponse(
        Long memberId,
        Long userId,
        String nickName,
        String profileImageUrl,
        LocalDateTime joinedAt
) {
    public static ChatRoomMemberResponse from(ChatRoomMember member, User user) {
        return new ChatRoomMemberResponse(
                member.getId(),
                user.getId(),
                user.getNickName(),
                user.getProfileImageUrl(),
                member.getJoinedAt()
        );
    }
}
