package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;
import com.back.catchmate.chat.domain.model.ChatRoomMember;

import java.time.LocalDateTime;

public record ChatRoomMemberResponse(
        Long memberId,
        Long userId,
        String nickName,
        String profileImageUrl,
        LocalDateTime joinedAt
) {
    public static ChatRoomMemberResponse from(ChatRoomMember member, ChatUserInfo user) {
        return new ChatRoomMemberResponse(
                member.getId(),
                user.userId(),
                user.nickName(),
                user.profileImageUrl(),
                member.getJoinedAt()
        );
    }
}
