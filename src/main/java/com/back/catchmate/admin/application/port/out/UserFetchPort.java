package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.user.domain.model.User;
import java.util.List;
import java.util.Map;

public interface UserFetchPort {
    List<User> getEventAlarmEnabledUsers();
    long getTotalUserCount();
    User getUser(Long userId);
    Map<String, Long> getUserCountByClub();
    long getUserCountByGender(Character gender);
    Map<String, Long> getUserCountByWatchStyle();
    DomainPage<User> getUsersByClub(String clubName, DomainPageable pageable);
    void updateUser(User user);
}
