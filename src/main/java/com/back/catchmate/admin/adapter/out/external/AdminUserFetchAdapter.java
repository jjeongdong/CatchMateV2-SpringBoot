package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.UserFetchPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.User;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public List<User> getEventAlarmEnabledUsers() {
        return userService.getEventAlarmEnabledUsers();
    }

    @Override
    public long getTotalUserCount() {
        return userService.getTotalUserCount();
    }

    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }

    @Override
    public List<User> getUsers(List<Long> userIds) {
        return userService.getUsers(userIds);
    }

    @Override
    public Map<String, Long> getUserCountByClub() {
        return userService.getUserCountByClub();
    }

    @Override
    public long getUserCountByGender(Character gender) {
        return userService.getUserCountByGender(gender);
    }

    @Override
    public Map<String, Long> getUserCountByWatchStyle() {
        return userService.getUserCountByWatchStyle();
    }

    @Override
    public Page<User> getUsersByClub(String clubName, Pageable pageable) {
        return userService.getUsersByClub(clubName, pageable);
    }

    @Override
    public void updateUser(User user) {
        userService.updateUser(user);
    }
}
