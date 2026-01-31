package com.back.catchmate.domain.user.repository;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;

import java.util.Map;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findById(Long id);

    DomainPage<User> findAllByClubName(String clubName, DomainPageable pageable);

    Map<String, Long> countUsersByClub();

    Map<String, Long> countUsersByWatchStyle();

    boolean existsByNickName(String nickName);

    long count();

    long countByGender(Character gender);
}
