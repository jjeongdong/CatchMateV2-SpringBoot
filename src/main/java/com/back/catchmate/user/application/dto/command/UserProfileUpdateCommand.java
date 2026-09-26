package com.back.catchmate.user.application.dto.command;

public record UserProfileUpdateCommand(String nickName, Long clubId, String watchStyle) {}
