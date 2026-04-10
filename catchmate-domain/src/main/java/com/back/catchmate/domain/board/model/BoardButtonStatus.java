package com.back.catchmate.domain.board.model;

import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.user.model.User;
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

    public static BoardButtonStatus resolve(User requestingUser, Board board, Optional<Enroll> enrollOptional) {
        if (board.getUser().getId().equals(requestingUser.getId())) {
            return VIEW_CHAT;
        }
        if (enrollOptional.isEmpty()) {
            return APPLY;
        }
        return switch (enrollOptional.get().getAcceptStatus()) {
            case ACCEPTED -> VIEW_CHAT;
            case PENDING -> CANCEL;
            case REJECTED -> REJECTED;
            default -> APPLY;
        };
    }
}
