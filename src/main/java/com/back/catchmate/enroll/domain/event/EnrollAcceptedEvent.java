package com.back.catchmate.enroll.domain.event;

public record EnrollAcceptedEvent(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {}
