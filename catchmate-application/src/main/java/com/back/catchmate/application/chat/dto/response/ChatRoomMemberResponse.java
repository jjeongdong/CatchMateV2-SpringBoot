package com.back.catchmate.application.chat.dto.response;

import com.back.catchmate.domain.chat.model.ChatRoomMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ChatRoomMemberResponse {
    private Long memberId;
    private Long userId;
    private String nickName;
    private String profileImageUrl;
    private LocalDateTime joinedAt;

    public static ChatRoomMemberResponse from(ChatRoomMember member) {
        return ChatRoomMemberResponse.builder()
                .memberId(member.getId())
                .userId(member.getUser().getId())
                .nickName(member.getUser().getNickName())
                .profileImageUrl(member.getUser().getProfileImageUrl())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
