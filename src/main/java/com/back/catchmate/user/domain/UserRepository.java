package com.back.catchmate.user.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    User getById(Long userId);

    List<User> findAllByIds(Collection<Long> userIds);

    Optional<User> findByProviderId(String providerId);

    boolean existsByNickName(String nickName);

    List<User> findAllEventAlarmEnabled();

    /** clubId 가 null 이면 전체. 최신 가입순. */
    List<User> findAllByClubId(Long clubId, long offset, int limit);

    /** clubId 가 null 이면 전체. */
    long countByClubId(Long clubId);

    Map<Long, Long> countGroupedByClubId();

    /** 응원 스타일이 없는 회원은 빠진다. */
    Map<String, Long> countGroupedByWatchStyle();

    long count();

    long countByGender(Character gender);
}
