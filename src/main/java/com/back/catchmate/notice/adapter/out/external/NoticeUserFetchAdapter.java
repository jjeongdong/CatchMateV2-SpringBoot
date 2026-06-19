package com.back.catchmate.notice.adapter.out.external;

import com.back.catchmate.notice.application.port.out.dto.NoticeUserInfo;
import com.back.catchmate.notice.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NoticeUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public NoticeUserInfo getUser(Long userId) {
        return toWriterProfile(userInternalQueryUseCase.getUser(userId));
    }

    @Override
    public List<NoticeUserInfo> getUsers(List<Long> userIds) {
        return userInternalQueryUseCase.getUsers(userIds).stream()
                .map(this::toWriterProfile)
                .toList();
    }

    private NoticeUserInfo toWriterProfile(UserInternalResponse response) {
        return new NoticeUserInfo(response.userId(), response.nickName());
    }
}
