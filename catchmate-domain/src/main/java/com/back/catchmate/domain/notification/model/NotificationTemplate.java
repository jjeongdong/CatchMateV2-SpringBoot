package com.back.catchmate.domain.notification.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationTemplate {
    
    // [모임 신청 관련]
    ENROLL_REQUEST("새로운 참여 신청", "'%s' 모임에 새로운 참여 신청이 도착했습니다."),
    ENROLL_ACCEPT("신청 수락 완료", "축하합니다! '%s' 모임 참여가 수락되었습니다. 채팅방을 확인해보세요."),
    ENROLL_REJECT("신청 거절 안내", "아쉽지만 '%s' 모임 참여 신청이 거절되었습니다."),

    // [채팅 관련]
    CHAT_NEW_MESSAGE("%s", "%s"), // 채팅은 발신자 닉네임이 제목, 메시지가 내용이 됨

    // [문의/공지 관련]
    INQUIRY_ANSWER("1:1 문의 답변 완료", "작성하신 1:1 문의에 답변이 등록되었습니다.");

    private final String title;
    private final String bodyTemplate;

    // 본문에 동적 데이터(게시글 제목 등)를 바인딩하는 메서드
    public String formatBody(Object... args) {
        return String.format(bodyTemplate, args);
    }

    // 제목에 동적 데이터(채팅 발신자 이름 등)를 바인딩하는 메서드
    public String formatTitle(Object... args) {
        return String.format(title, args);
    }
}
