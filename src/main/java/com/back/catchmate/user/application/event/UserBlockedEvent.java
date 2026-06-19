package com.back.catchmate.user.application.event;

public record UserBlockedEvent(Long blockerId, Long blockedId) {
    public static UserBlockedEvent of(Long blockerId, Long blockedId) {
        return new UserBlockedEvent(blockerId, blockedId);
    }
}
