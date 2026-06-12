package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.notification.application.port.out.UserFetchPort;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }

    @Override
    public List<User> getUsers(List<Long> userIds) {
        return userService.getUsers(userIds);
    }
}
