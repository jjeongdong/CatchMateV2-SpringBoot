package com.back.catchmate.enroll.application.port.out.external;

import com.back.catchmate.enroll.application.port.out.dto.EnrollUserInfo;

import java.util.List;

public interface UserFetchPort {
    EnrollUserInfo getUser(Long userId);

    List<EnrollUserInfo> getUsers(List<Long> userIds);
}
