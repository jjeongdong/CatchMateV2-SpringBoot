package com.back.catchmate.domain.user.repository;

import com.back.catchmate.domain.user.model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByProviderId(String providerId);
    Optional<User> findById(Long id);
    boolean existsByNickName(String nickName);
}
