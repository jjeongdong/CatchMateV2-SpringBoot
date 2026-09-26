package com.back.catchmate.user.application.dto.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserAlarmType;
import com.back.catchmate.user.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserInfoTest {

    @Test
    @DisplayName("엔티티의 알림 설정과 신고 여부를 옮겨 담는다")
    void fromCopiesFlags() {
        // given
        User user = UserFixture.user(1L, 3L);
        user.updateAlarm(UserAlarmType.ENROLL, false);
        user.markAsReported();
        user.updateFcmToken("token");

        // when
        UserInfo info = UserInfo.from(user);

        // then
        assertThat(info.chatAlarmEnabled()).isTrue();
        assertThat(info.enrollAlarmEnabled()).isFalse();
        assertThat(info.eventAlarmEnabled()).isTrue();
        assertThat(info.reported()).isTrue();
        assertThat(info.fcmToken()).isEqualTo("token");
        assertThat(info.nickName()).isEqualTo(user.getNickName());
    }
}
