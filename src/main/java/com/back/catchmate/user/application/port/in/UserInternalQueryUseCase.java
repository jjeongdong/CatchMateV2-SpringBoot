package com.back.catchmate.user.application.port.in;

import com.back.catchmate.user.application.dto.response.UserInternalResponse;

import java.util.List;
import java.util.Optional;

public interface UserInternalQueryUseCase {
    UserInternalResponse getUser(Long userId);

    Optional<UserInternalResponse> findByProviderId(String providerIdWithProvider);

    List<UserInternalResponse> getUsers(List<Long> userIds);

    List<UserInternalResponse> getEventAlarmEnabledUsers();
}
