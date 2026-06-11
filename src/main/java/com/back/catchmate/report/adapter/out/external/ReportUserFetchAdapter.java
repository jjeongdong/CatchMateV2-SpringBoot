package com.back.catchmate.report.adapter.out.external;

import com.back.catchmate.report.application.port.out.UserFetchPort;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }
}
