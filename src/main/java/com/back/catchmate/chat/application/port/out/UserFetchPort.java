package com.back.catchmate.chat.application.port.out;

import com.back.catchmate.user.domain.model.User;

public interface UserFetchPort {
    User getUser(Long userId);
}
