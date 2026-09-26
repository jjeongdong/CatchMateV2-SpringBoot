package com.back.catchmate.enroll.domain.event;

public record EnrollRequestedEvent(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {}
