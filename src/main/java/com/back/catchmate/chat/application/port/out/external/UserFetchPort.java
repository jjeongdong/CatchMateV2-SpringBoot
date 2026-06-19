package com.back.catchmate.chat.application.port.out.external;

import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;

import java.util.List;

public interface UserFetchPort {
    ChatUserInfo getUser(Long userId);

    List<ChatUserInfo> getUsers(List<Long> userIds);
}
