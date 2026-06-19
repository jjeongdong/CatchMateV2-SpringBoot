package com.back.catchmate.notice.application.port.out.external;

import com.back.catchmate.notice.application.port.out.dto.NoticeUserInfo;

import java.util.List;

public interface UserFetchPort {
    NoticeUserInfo getUser(Long userId);

    List<NoticeUserInfo> getUsers(List<Long> userIds);
}
