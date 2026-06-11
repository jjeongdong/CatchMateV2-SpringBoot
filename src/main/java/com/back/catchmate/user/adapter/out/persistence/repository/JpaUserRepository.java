package com.back.catchmate.user.adapter.out.persistence.repository;

import com.back.catchmate.user.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByProviderId(String providerId);

    boolean existsByNickName(String nickName);

    long countByGender(Character gender);
}
