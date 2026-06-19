package com.back.catchmate.chat.adapter.out.external;

import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;
import com.back.catchmate.chat.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public ChatUserInfo getUser(Long userId) {
        return fromInternalResponse(userInternalQueryUseCase.getUser(userId));
    }

    @Override
    public List<ChatUserInfo> getUsers(List<Long> userIds) {
        return userInternalQueryUseCase.getUsers(userIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private ChatUserInfo fromInternalResponse(UserInternalResponse response) {
        if (response == null) return null;
        return new ChatUserInfo(
                response.userId(),
                response.nickName(),
                response.profileImageUrl(),
                response.fcmToken(),
                response.chatAlarmEnabled(),
                response.clubId()
        );
    }
}
