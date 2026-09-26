package com.back.catchmate.inquiry.fixture;

import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.inquiry.domain.InquiryType;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class InquiryFixture {

    public static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 1, 12, 0);

    private InquiryFixture() {}

    public static Inquiry waiting(Long inquiryId, Long userId) {
        Inquiry inquiry = Inquiry.create(userId, InquiryType.ACCOUNT, "로그인이 안 돼요");
        // 저장 없이 쓰는 단위 테스트용이라 id·생성 시각을 리플렉션으로 채운다.
        ReflectionTestUtils.setField(inquiry, "id", inquiryId);
        ReflectionTestUtils.setField(inquiry, "createdAt", CREATED_AT);
        return inquiry;
    }

    public static Inquiry answered(Long inquiryId, Long userId, String answer) {
        Inquiry inquiry = waiting(inquiryId, userId);
        inquiry.registerAnswer(answer);
        return inquiry;
    }
}
