package com.back.catchmate.board.domain.model;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum BoardButtonStatus {
    VIEW_CHAT("채팅방 보기"),
    APPLY("신청하기"),
    CANCEL("신청 취소"),
    REJECTED("거절됨");

    private final String description;

    public static BoardButtonStatus resolve(Long requestingUserId, Board board, Optional<AcceptStatus> enrollAcceptStatus) {
        if (board.getUserId().equals(requestingUserId)) {
            return VIEW_CHAT;
        }
        if (enrollAcceptStatus.isEmpty()) {
            return APPLY;
        }
        return switch (enrollAcceptStatus.get()) {
            case ACCEPTED -> VIEW_CHAT;
            case PENDING -> CANCEL;
            case REJECTED -> REJECTED;
            default -> APPLY;
        };
    }
}
