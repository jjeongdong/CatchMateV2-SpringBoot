package com.back.catchmate.oauth.application.port.out.external;

import com.back.catchmate.oauth.application.dto.response.RegisteredUserSummary;

import java.util.Optional;

public interface UserFetchPort {
    Optional<RegisteredUserSummary> findByProviderId(String providerIdWithProvider);
}
