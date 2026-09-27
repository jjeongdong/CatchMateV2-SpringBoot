package com.back.catchmate.notification.application;

import static com.back.catchmate.notification.fixture.NotificationFixture.NOW;
import static com.back.catchmate.notification.fixture.NotificationFixture.board;
import static com.back.catchmate.notification.fixture.NotificationFixture.notification;
import static com.back.catchmate.notification.fixture.NotificationFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.enroll.application.EnrollQueryApi;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.notification.application.dto.result.NotificationResult;
import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.notification.domain.exception.NotificationNotOwnerException;
import com.back.catchmate.user.application.UserQueryApi;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private BoardQueryApi boardQueryApi;

    @Mock
    private GameQueryApi gameQueryApi;

    @Mock
    private ClubQueryApi clubQueryApi;

    @Mock
    private EnrollQueryApi enrollQueryApi;

    @InjectMocks
    private NotificationQueryService service;

    @Test
    @DisplayName("목록은 size+1 로 다음 페이지를 판단하고, 타 BC 정보는 BC 마다 한 번씩 모아 조합한다")
    void getNotifications() {
        // given
        Notification enroll = notification(3L, 1L, 7L, 10L, AlarmType.ENROLL, 100L);
        Notification event = notification(2L, 1L, null, null, AlarmType.EVENT, 5L);
        Notification overflow = notification(1L, 1L, 8L, 11L, AlarmType.ENROLL, 101L);
        given(notificationRepository.findPageByUserId(1L, null, null, 3)).willReturn(List.of(enroll, event, overflow));
        given(enrollQueryApi.getAcceptStatuses(List.of(100L))).willReturn(Map.of(100L, "ACCEPTED"));
        given(boardQueryApi.getInfos(List.of(10L))).willReturn(Map.of(10L, board(10L, "직관", 20L)));
        given(gameQueryApi.getInfos(List.of(20L)))
                .willReturn(Map.of(20L, new GameInfo(20L, LocalDateTime.of(2026, 9, 5, 18, 30), "잠실", 1L, 2L)));
        given(clubQueryApi.getInfos(List.of(1L, 2L)))
                .willReturn(Map.of(
                        1L, new ClubInfo(1L, "LG", "잠실", "서울"),
                        2L, new ClubInfo(2L, "두산", "잠실", "서울")));
        given(userQueryApi.getInfos(List.of(7L))).willReturn(Map.of(7L, user(7L, "철수", null, true, true, true)));

        // when
        CursorPageResult<NotificationResult> page = service.getNotifications(1L, null, 2);

        // then
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(new NotificationCursor(NOW, 2L).encode());
        assertThat(page.content()).hasSize(2);
        NotificationResult first = page.content().get(0);
        assertThat(first.acceptStatus()).isEqualTo("ACCEPTED");
        assertThat(first.gameInfo()).isEqualTo("2026.09.05 18:30 · 잠실 · LG vs 두산");
        assertThat(first.senderNickname()).isEqualTo("철수");
        assertThat(first.alarmType()).isEqualTo("ENROLL");
        NotificationResult second = page.content().get(1);
        assertThat(second.acceptStatus()).isNull();
        assertThat(second.gameInfo()).isNull();
        assertThat(second.senderNickname()).isNull();
    }

    @Test
    @DisplayName("커서를 풀어 그 다음부터 조회하고, 비어 있으면 타 BC 를 부르지 않는다")
    void getNotificationsWithCursor() {
        String cursor = new NotificationCursor(NOW, 9L).encode();
        given(notificationRepository.findPageByUserId(1L, NOW, 9L, 21)).willReturn(List.of());

        CursorPageResult<NotificationResult> page = service.getNotifications(1L, cursor, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        then(enrollQueryApi).shouldHaveNoInteractions();
        then(boardQueryApi).shouldHaveNoInteractions();
        then(userQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("게시글이 삭제됐거나 발신자가 없어도 상세 조회는 실패하지 않는다 — 경기 정보는 빈 문자열, 발신자는 비움")
    void missingBoardAndSender() {
        given(notificationRepository.getById(3L)).willReturn(notification(3L, 1L, 7L, 10L, AlarmType.ENROLL, 100L));
        given(enrollQueryApi.getAcceptStatuses(List.of(100L))).willReturn(Map.of());
        given(boardQueryApi.getInfos(List.of(10L))).willReturn(Map.of());
        given(userQueryApi.getInfos(List.of(7L))).willReturn(Map.of());

        NotificationResult result = service.getNotification(1L, 3L);

        assertThat(result.gameInfo()).isEmpty();
        assertThat(result.senderNickname()).isNull();
        assertThat(result.acceptStatus()).isNull();
        then(gameQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("본인 알림이 아니면 상세 조회를 거부하고 타 BC 를 부르지 않는다")
    void rejectsOtherUsersNotification() {
        given(notificationRepository.getById(3L)).willReturn(notification(3L, 2L, 7L, 10L, AlarmType.ENROLL, 100L));

        assertThatThrownBy(() -> service.getNotification(1L, 3L)).isInstanceOf(NotificationNotOwnerException.class);
        then(enrollQueryApi).shouldHaveNoInteractions();
        then(userQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("안 읽은 알림 여부")
    void getUnread() {
        given(notificationRepository.existsUnreadByUserId(1L)).willReturn(true);

        assertThat(service.getUnread(1L).hasUnread()).isTrue();
    }
}
