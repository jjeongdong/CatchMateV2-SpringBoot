package com.back.catchmate.user.application.port.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.domain.model.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findById(Long id);

    List<User> findAllByIds(List<Long> ids);

    List<User> findAllEventAlarmEnabled();

    Page<User> findAllByClubId(Long clubId, Pageable pageable);

    Map<Long, Long> countUsersGroupedByClubId();

    Map<String, Long> countUsersByWatchStyle();

    boolean existsByNickName(String nickName);

    long count();

    long countByGender(Character gender);
}
