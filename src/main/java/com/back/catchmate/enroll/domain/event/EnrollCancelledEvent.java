package com.back.catchmate.enroll.domain.event;

public record EnrollCancelledEvent(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {}
