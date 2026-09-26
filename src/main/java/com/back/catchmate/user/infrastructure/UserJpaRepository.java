package com.back.catchmate.user.infrastructure;

import com.back.catchmate.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderId(String providerId);

    boolean existsByNickName(String nickName);

    long countByGender(Character gender);
}
