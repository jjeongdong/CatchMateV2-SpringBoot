package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.enroll.application.port.out.external.UserFetchPort;
import com.back.catchmate.enroll.application.port.out.dto.EnrollUserInfo;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnrollUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public EnrollUserInfo getUser(Long userId) {
        return toEnrollUserInfo(userInternalQueryUseCase.getUser(userId));
    }

    @Override
    public List<EnrollUserInfo> getUsers(List<Long> userIds) {
        return userInternalQueryUseCase.getUsers(userIds).stream()
                .map(this::toEnrollUserInfo)
                .toList();
    }

    private EnrollUserInfo toEnrollUserInfo(UserInternalResponse response) {
        return new EnrollUserInfo(
                response.userId(),
                response.clubId(),
                response.nickName(),
                response.email(),
                response.profileImageUrl(),
                response.gender(),
                response.birthDate(),
                response.watchStyle(),
                response.authority()
        );
    }
}
