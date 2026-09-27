package com.back.catchmate.notification.application;

import static com.back.catchmate.notification.fixture.NotificationFixture.notification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.notification.domain.exception.NotificationNotOwnerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCommandService service;

    @Test
    @DisplayName("본인 알림을 읽음 처리한다")
    void marksAsRead() {
        Notification target = notification(3L, 1L, null, null, AlarmType.EVENT, 5L);
        given(notificationRepository.getById(3L)).willReturn(target);

        service.markNotificationAsRead(1L, 3L);

        assertThat(target.isRead()).isTrue();
    }

    @Test
    @DisplayName("남의 알림은 읽음·삭제 모두 거부한다")
    void rejectsOthers() {
        Notification target = notification(3L, 2L, null, null, AlarmType.EVENT, 5L);
        given(notificationRepository.getById(3L)).willReturn(target);

        assertThatThrownBy(() -> service.markNotificationAsRead(1L, 3L))
                .isInstanceOf(NotificationNotOwnerException.class);
        assertThatThrownBy(() -> service.deleteNotification(1L, 3L)).isInstanceOf(NotificationNotOwnerException.class);
        assertThat(target.isRead()).isFalse();
        then(notificationRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("본인 알림을 삭제한다")
    void deletes() {
        Notification target = notification(3L, 1L, null, null, AlarmType.EVENT, 5L);
        given(notificationRepository.getById(3L)).willReturn(target);

        service.deleteNotification(1L, 3L);

        then(notificationRepository).should().delete(target);
    }

    @Test
    @DisplayName("전체 읽음은 갱신 건수를 돌려준다")
    void readsAll() {
        given(notificationRepository.markAllReadByUserId(1L)).willReturn(4);

        assertThat(service.readAllNotifications(1L).updatedCount()).isEqualTo(4);
    }
}
