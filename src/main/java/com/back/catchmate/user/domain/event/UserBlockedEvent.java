package com.back.catchmate.user.domain.event;

public record UserBlockedEvent(Long blockerId, Long blockedId) {}
