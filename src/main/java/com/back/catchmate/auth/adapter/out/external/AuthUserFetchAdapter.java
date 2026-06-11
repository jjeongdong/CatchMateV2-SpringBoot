package com.back.catchmate.auth.adapter.out.external;

import com.back.catchmate.auth.application.port.out.UserFetchPort;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }

    @Override
    public void updateUser(User user) {
        userService.updateUser(user);
    }
}
