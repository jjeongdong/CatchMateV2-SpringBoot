package com.back.catchmate.user.event;

public record UserBlockedEvent(Long blockerId, Long blockedId) {
    public static UserBlockedEvent of(Long blockerId, Long blockedId) {
        return new UserBlockedEvent(blockerId, blockedId);
    }
}
