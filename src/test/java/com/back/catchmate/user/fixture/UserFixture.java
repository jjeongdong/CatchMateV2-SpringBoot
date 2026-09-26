package com.back.catchmate.user.fixture;

import com.back.catchmate.user.domain.User;
import java.time.LocalDate;
import org.springframework.test.util.ReflectionTestUtils;

public final class UserFixture {

    private UserFixture() {}

    public static User user(Long userId, Long clubId) {
        User user = newUser("KAKAO_" + userId, clubId);
        // 저장 없이 쓰는 단위 테스트용이라 id 를 리플렉션으로 채운다.
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    public static User newUser(String providerId, Long clubId) {
        return User.create(
                "KAKAO",
                providerId,
                providerId + "@catchmate.com",
                providerId,
                'M',
                LocalDate.of(2000, 1, 1),
                clubId,
                "https://example.com/profile.png",
                "응원형");
    }
}
