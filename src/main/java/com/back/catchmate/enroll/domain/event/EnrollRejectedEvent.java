package com.back.catchmate.enroll.domain.event;

public record EnrollRejectedEvent(Long enrollId, Long boardId, Long applicantId, Long boardOwnerId) {}
