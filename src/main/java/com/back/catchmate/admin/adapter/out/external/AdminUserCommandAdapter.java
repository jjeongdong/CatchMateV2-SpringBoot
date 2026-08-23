package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.external.UserCommandPort;
import com.back.catchmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserCommandAdapter implements UserCommandPort {
    private final UserService userService;

    @Override
    public void markUserAsReported(Long userId) {
        userService.markUserAsReported(userId);
    }
}
