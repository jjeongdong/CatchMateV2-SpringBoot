package com.back.catchmate.oauth.application.port.out;

import com.back.catchmate.user.domain.model.User;

import java.util.Optional;

public interface UserFetchPort {
    Optional<User> findByProviderId(String providerIdWithProvider);
    User createUser(User user);
}
