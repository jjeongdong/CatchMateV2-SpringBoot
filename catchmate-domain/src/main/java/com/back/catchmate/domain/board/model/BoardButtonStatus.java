package com.back.catchmate.domain.board.model; // 패키지는 프로젝트 구조에 맞게 조정하세요.

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardButtonStatus {
    VIEW_CHAT("채팅방 보기"),
    APPLY("신청하기"),
    CANCEL("신청 취소"),
    REJECTED("거절됨");

    private final String description; // 필요 시 클라이언트에게 상태에 대한 설명을 내려줄 때 활용 가능
}
