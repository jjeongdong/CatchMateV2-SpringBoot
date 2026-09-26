package com.back.catchmate.enroll.fixture;

import com.back.catchmate.enroll.domain.Enroll;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class EnrollFixture {

    public static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 9, 1, 12, 0);

    private EnrollFixture() {}

    public static Enroll pending(Long enrollId, Long applicantId, Long boardId, Long writerId) {
        Enroll enroll = Enroll.create(applicantId, boardId, writerId, "같이 가요");
        // 저장 없이 쓰는 단위 테스트용이라 id·생성 시각을 리플렉션으로 채운다.
        ReflectionTestUtils.setField(enroll, "id", enrollId);
        ReflectionTestUtils.setField(enroll, "createdAt", REQUESTED_AT);
        return enroll;
    }
}
