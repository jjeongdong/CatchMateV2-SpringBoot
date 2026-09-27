package com.back.catchmate.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.notification.domain.exception.NotificationNotOwnerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    @DisplayName("새 알림은 읽지 않은 상태다")
    void createsUnread() {
        Notification notification = Notification.create(1L, 2L, 10L, "제목", AlarmType.ENROLL, 100L);

        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getUserId()).isEqualTo(1L);
        assertThat(notification.getTargetId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("본인 알림이 아니면 NotificationNotOwnerException")
    void verifiesOwner() {
        Notification notification = Notification.create(1L, null, null, "제목", AlarmType.EVENT, 5L);

        assertThatCode(() -> notification.verifyOwner(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> notification.verifyOwner(2L)).isInstanceOf(NotificationNotOwnerException.class);
    }

    @Test
    @DisplayName("읽음 처리하면 read 가 true 가 된다")
    void marksAsRead() {
        Notification notification = Notification.create(1L, null, null, "제목", AlarmType.EVENT, 5L);

        notification.markAsRead();

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("신청 상태를 조회할 대상은 신청 알림이면서 대상 id 가 있는 것뿐이다")
    void detectsEnroll() {
        assertThat(Notification.create(1L, 2L, 10L, "t", AlarmType.ENROLL, 100L).isEnroll())
                .isTrue();
        assertThat(Notification.create(1L, 2L, 10L, "t", AlarmType.ENROLL, null).isEnroll())
                .isFalse();
        assertThat(Notification.create(1L, 2L, 10L, "t", AlarmType.CHAT, 100L).isEnroll())
                .isFalse();
    }
}
