package com.back.catchmate.user.application.port.in;

import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface UserAdminQueryUseCase {
    Page<UserInternalResponse> getUsersByClubId(Long clubId, Pageable pageable);

    Map<Long, Long> getUserCountByClubId();

    Map<String, Long> getUserCountByWatchStyle();

    long getTotalUserCount();

    long getUserCountByGender(Character gender);
}
