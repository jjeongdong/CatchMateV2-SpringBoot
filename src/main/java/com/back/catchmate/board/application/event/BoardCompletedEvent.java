package com.back.catchmate.board.application.event;

/**
 * 게시글이 작성 완료 상태로 게시되었음을 알리는 사실 이벤트.
 */
public record BoardCompletedEvent(
        Long boardId,
        Long ownerId
) {
    public static BoardCompletedEvent of(Long boardId, Long ownerId) {
        return new BoardCompletedEvent(boardId, ownerId);
    }
}
