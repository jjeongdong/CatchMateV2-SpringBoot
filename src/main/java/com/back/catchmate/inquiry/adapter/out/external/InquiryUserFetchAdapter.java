package com.back.catchmate.inquiry.adapter.out.external;

import com.back.catchmate.inquiry.application.port.out.UserFetchPort;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }
}
