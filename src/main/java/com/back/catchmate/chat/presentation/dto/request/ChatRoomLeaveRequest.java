package com.back.catchmate.chat.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatRoomLeaveRequest(@NotNull(message = "채팅방 ID는 필수입니다.") Long chatRoomId) {}
