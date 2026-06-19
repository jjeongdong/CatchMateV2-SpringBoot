package com.back.catchmate.board.application.port.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardUserInfo;

import java.util.List;

public interface UserFetchPort {
    BoardUserInfo getUser(Long userId);

    List<BoardUserInfo> getUsers(List<Long> userIds);
}
