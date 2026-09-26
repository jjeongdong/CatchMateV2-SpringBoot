package com.back.catchmate.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.user.fixture.UserFixture;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("생성하면 알림 4종이 켜지고 일반 권한·미신고·FCM 토큰 없음으로 시작한다")
    void createSetsDefaults() {
        // when
        User user = User.create(
                "KAKAO", "KAKAO_1", "a@catchmate.com", "홍길동", 'M', LocalDate.of(2000, 1, 1), 3L, "url", "응원형");

        // then
        assertThat(user.getProvider()).isEqualTo("KAKAO");
        assertThat(user.getProviderId()).isEqualTo("KAKAO_1");
        assertThat(user.getClubId()).isEqualTo(3L);
        assertThat(user.isAllAlarmEnabled()).isTrue();
        assertThat(user.isChatAlarmEnabled()).isTrue();
        assertThat(user.isEnrollAlarmEnabled()).isTrue();
        assertThat(user.isEventAlarmEnabled()).isTrue();
        assertThat(user.getAuthority()).isEqualTo(Authority.ROLE_USER);
        assertThat(user.isReported()).isFalse();
        assertThat(user.getFcmToken()).isNull();
    }

    @Test
    @DisplayName("프로필 수정은 null 인 항목을 바꾸지 않는다")
    void updateProfileIgnoresNulls() {
        // given
        User user = UserFixture.user(1L, 3L);
        String originalNickName = user.getNickName();

        // when
        user.updateProfile(null, "직관형", null, "new-url");

        // then
        assertThat(user.getNickName()).isEqualTo(originalNickName);
        assertThat(user.getWatchStyle()).isEqualTo("직관형");
        assertThat(user.getClubId()).isEqualTo(3L);
        assertThat(user.getProfileImageUrl()).isEqualTo("new-url");
    }

    @Nested
    @DisplayName("알림 설정")
    class UpdateAlarm {

        @Test
        @DisplayName("ALL 은 4종을 모두 바꾼다")
        void allChangesEveryAlarm() {
            // given
            User user = UserFixture.user(1L, 3L);

            // when
            user.updateAlarm(UserAlarmType.ALL, false);

            // then
            assertThat(user.isAllAlarmEnabled()).isFalse();
            assertThat(user.isChatAlarmEnabled()).isFalse();
            assertThat(user.isEnrollAlarmEnabled()).isFalse();
            assertThat(user.isEventAlarmEnabled()).isFalse();
        }

        @Test
        @DisplayName("개별 종류는 그 알림만 바꾼다")
        void singleTypeChangesOnlyItself() {
            // given
            User user = UserFixture.user(1L, 3L);

            // when
            user.updateAlarm(UserAlarmType.CHAT, false);

            // then
            assertThat(user.isChatAlarmEnabled()).isFalse();
            assertThat(user.isAllAlarmEnabled()).isTrue();
            assertThat(user.isEnrollAlarmEnabled()).isTrue();
            assertThat(user.isEventAlarmEnabled()).isTrue();
        }
    }

    @Test
    @DisplayName("FCM 토큰을 등록하고 지울 수 있다")
    void updatesAndClearsFcmToken() {
        // given
        User user = UserFixture.user(1L, 3L);

        // when
        user.updateFcmToken("token-1");
        String registered = user.getFcmToken();
        user.clearFcmToken();

        // then
        assertThat(registered).isEqualTo("token-1");
        assertThat(user.getFcmToken()).isNull();
    }

    @Test
    @DisplayName("신고 처리하면 reported 가 켜진다")
    void markAsReported() {
        // given
        User user = UserFixture.user(1L, 3L);

        // when
        user.markAsReported();

        // then
        assertThat(user.isReported()).isTrue();
    }
}
