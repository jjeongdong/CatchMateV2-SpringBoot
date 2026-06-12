package com.back.catchmate.notification.application.port.out;

import com.back.catchmate.user.domain.model.User;

import java.util.List;

public interface UserFetchPort {
    User getUser(Long userId);
    List<User> getUsers(List<Long> userIds);
}
