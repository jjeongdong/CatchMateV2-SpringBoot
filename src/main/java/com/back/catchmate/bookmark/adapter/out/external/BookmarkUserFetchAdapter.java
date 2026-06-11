package com.back.catchmate.bookmark.adapter.out.external;

import com.back.catchmate.bookmark.application.port.out.UserFetchPort;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookmarkUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public User getUser(Long userId) {
        return userService.getUser(userId);
    }
}
