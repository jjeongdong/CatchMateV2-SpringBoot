package com.back.catchmate.user.repository;

import com.back.catchmate.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    Optional<User> findByProviderId(String providerId);

    boolean existsByNickName(String nickName);

    long countByGender(Character gender);
}
