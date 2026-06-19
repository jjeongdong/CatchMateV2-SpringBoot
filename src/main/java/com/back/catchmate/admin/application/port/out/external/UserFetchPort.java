package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface UserFetchPort {
    AdminUserInfo getUser(Long userId);

    List<AdminUserInfo> getUsers(List<Long> userIds);

    Page<AdminUserInfo> getUsersByClubId(Long clubId, Pageable pageable);

    Map<Long, Long> getUserCountByClubId();

    Map<String, Long> getUserCountByWatchStyle();

    long getTotalUserCount();

    long getUserCountByGender(Character gender);
}
