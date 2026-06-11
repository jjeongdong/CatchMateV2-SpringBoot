package com.back.catchmate.admin.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    Page<User> getUsersByClub(String clubName, Pageable pageable);
    void updateUser(User user);
}
