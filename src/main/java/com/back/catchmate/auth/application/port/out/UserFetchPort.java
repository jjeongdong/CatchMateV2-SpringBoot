package com.back.catchmate.auth.application.port.out;

import com.back.catchmate.user.domain.model.User;

public interface UserFetchPort {
    User getUser(Long userId);
    void updateUser(User user);
}
