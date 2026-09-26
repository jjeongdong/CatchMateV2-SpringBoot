package com.back.catchmate.board.domain;

public enum BoardButtonStatus {
    VIEW_CHAT,
    APPLY,
    CANCEL,
    REJECTED;

    // enrollAcceptStatus: 조회자의 신청 상태 이름 (신청이 없으면 null)
    public static BoardButtonStatus resolve(Long requesterId, Long writerId, String enrollAcceptStatus) {
        if (writerId.equals(requesterId)) {
            return VIEW_CHAT;
        }
        if (enrollAcceptStatus == null) {
            return APPLY;
        }
        return switch (enrollAcceptStatus) {
            case "ACCEPTED" -> VIEW_CHAT;
            case "PENDING" -> CANCEL;
            case "REJECTED" -> REJECTED;
            default -> APPLY;
        };
    }
}
