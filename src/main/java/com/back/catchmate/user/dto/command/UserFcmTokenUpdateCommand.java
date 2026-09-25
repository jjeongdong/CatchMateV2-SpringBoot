package com.back.catchmate.user.dto.command;

public record UserFcmTokenUpdateCommand(Long userId, String fcmToken) {}
