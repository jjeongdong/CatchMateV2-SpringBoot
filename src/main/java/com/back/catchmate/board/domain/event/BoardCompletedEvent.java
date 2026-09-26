package com.back.catchmate.board.domain.event;

/** 게시글이 작성 완료(발행) 상태가 되었음을 알리는 사실 이벤트. */
public record BoardCompletedEvent(Long boardId, Long ownerId) {}
