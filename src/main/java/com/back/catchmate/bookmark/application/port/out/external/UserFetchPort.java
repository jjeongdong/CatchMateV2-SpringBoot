package com.back.catchmate.bookmark.application.port.out.external;

import com.back.catchmate.bookmark.application.port.out.dto.BookmarkUserInfo;

import java.util.List;

public interface UserFetchPort {
    List<BookmarkUserInfo> getUsers(List<Long> userIds);
}
