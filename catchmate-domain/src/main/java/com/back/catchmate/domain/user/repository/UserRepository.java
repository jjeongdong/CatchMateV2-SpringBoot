package com.back.catchmate.domain.user.repository;

import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.user.model.User;

import java.util.Map;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByProviderId(String providerId);
    Optional<User> findById(Long id);
    boolean existsByNickName(String nickName);

    // 대시보드
    long count();
    long countByGender(Character gender);
    Map<String, Long> countUsersByClub();
    Map<String, Long> countUsersByWatchStyle();
    DomainPage<User> findAllByClubName(String clubName, DomainPageable pageable);
}
