package com.back.catchmate.infrastructure.persistence.user.repository;

import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByProviderId(String providerId);
    boolean existsByNickName(String nickName);
}
