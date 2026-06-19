package com.back.catchmate.auth.application.port.out.external;

public interface UserCommandPort {
    void clearFcmToken(Long userId);
}
