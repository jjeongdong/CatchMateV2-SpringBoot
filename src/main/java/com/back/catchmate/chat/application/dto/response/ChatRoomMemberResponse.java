package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.domain.model.ChatRoomMember;
import java.time.LocalDateTime;

public record ChatRoomMemberResponse(
        Long memberId,
        Long userId,
        String nickName,
        String profileImageUrl,
        LocalDateTime joinedAt
) {
    public static ChatRoomMemberResponse from(ChatRoomMember member) {
        return new ChatRoomMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getNickName(),
                member.getUser().getProfileImageUrl(),
                member.getJoinedAt()
        );
    }
}
