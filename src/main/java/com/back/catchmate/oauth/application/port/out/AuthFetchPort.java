package com.back.catchmate.oauth.application.port.out;

import com.back.catchmate.auth.domain.model.AuthToken;
import com.back.catchmate.user.domain.model.User;

public interface AuthFetchPort {
    AuthToken createToken(User user);
}
