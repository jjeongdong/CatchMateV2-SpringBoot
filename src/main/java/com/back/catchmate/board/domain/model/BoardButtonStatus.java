package com.back.catchmate.board.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardButtonStatus {
    VIEW_CHAT("채팅방 보기"),
    APPLY("신청하기"),
    CANCEL("신청 취소"),
    REJECTED("거절됨");

    private final String description;

    public static BoardButtonStatus resolve(Long requestingUserId, Board board, String enrollAcceptStatus) {
        // 1. 게시글 작성자 본인인 경우
        if (board.getUserId().equals(requestingUserId)) {
            return VIEW_CHAT;
        }

        // 2. 신청 내역이 아예 없는 경우 (호출부에서 null을 넘김)
        if (enrollAcceptStatus == null) {
            return APPLY;
        }

        // 3. 신청 내역이 있는 경우, 상태에 따라 버튼 매핑
        return switch (enrollAcceptStatus) {
            case "ACCEPTED" -> VIEW_CHAT;
            case "PENDING" -> CANCEL;
            case "REJECTED" -> REJECTED;
            default -> APPLY;
        };
    }
}
