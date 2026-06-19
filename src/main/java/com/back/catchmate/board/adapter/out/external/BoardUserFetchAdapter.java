package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardUserInfo;
import com.back.catchmate.board.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public BoardUserInfo getUser(Long userId) {
        return toBoardUserInfo(userInternalQueryUseCase.getUser(userId));
    }

    @Override
    public List<BoardUserInfo> getUsers(List<Long> userIds) {
        return userInternalQueryUseCase.getUsers(userIds).stream()
                .map(this::toBoardUserInfo)
                .toList();
    }

    private BoardUserInfo toBoardUserInfo(UserInternalResponse response) {
        return new BoardUserInfo(
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
