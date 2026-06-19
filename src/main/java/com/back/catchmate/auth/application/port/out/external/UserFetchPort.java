package com.back.catchmate.auth.application.port.out.external;

import com.back.catchmate.auth.application.port.out.dto.AuthUserInfo;

public interface UserFetchPort {
    AuthUserInfo getUser(Long userId);
}
