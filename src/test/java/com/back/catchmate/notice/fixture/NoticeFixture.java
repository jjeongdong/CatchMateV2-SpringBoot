package com.back.catchmate.notice.fixture;

import com.back.catchmate.notice.domain.Notice;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class NoticeFixture {

    public static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 1, 12, 0);

    private NoticeFixture() {}

    public static Notice notice(Long noticeId, Long writerId) {
        Notice notice = Notice.create(writerId, "공지 " + noticeId, "내용 " + noticeId);
        // 저장 없이 쓰는 단위 테스트용이라 id·생성 시각을 리플렉션으로 채운다.
        ReflectionTestUtils.setField(notice, "id", noticeId);
        ReflectionTestUtils.setField(notice, "createdAt", CREATED_AT);
        return notice;
    }
}
