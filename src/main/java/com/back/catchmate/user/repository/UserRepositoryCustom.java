package com.back.catchmate.user.repository;

import com.back.catchmate.user.entity.User;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    List<User> findAllEventAlarmEnabled();

    Page<User> findAllByClubId(Long clubId, Pageable pageable);

    Map<Long, Long> countUsersGroupedByClubId();

    Map<String, Long> countUsersByWatchStyle();
}
