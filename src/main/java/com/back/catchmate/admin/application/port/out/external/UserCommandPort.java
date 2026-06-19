package com.back.catchmate.admin.application.port.out.external;

public interface UserCommandPort {
    void markUserAsReported(Long userId);
}
