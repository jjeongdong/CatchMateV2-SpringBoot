package com.back.catchmate.bookmark.adapter.out.external;

import com.back.catchmate.bookmark.application.port.out.dto.BookmarkUserInfo;
import com.back.catchmate.bookmark.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookmarkUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public List<BookmarkUserInfo> getUsers(List<Long> userIds) {
        List<UserInternalResponse> responses = userInternalQueryUseCase.getUsers(userIds);

        return responses.stream()
                .map(response -> new BookmarkUserInfo(
                        response.userId(),
                        response.clubId(),
                        response.nickName(),
                        response.profileImageUrl()
                ))
                .collect(Collectors.toList());
    }
}
