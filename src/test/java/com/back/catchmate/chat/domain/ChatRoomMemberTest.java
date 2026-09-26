package com.back.catchmate.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.chat.domain.exception.ChatRoomMemberNotFoundException;
import com.back.catchmate.chat.domain.exception.ChatRoomReadOnlyException;
import com.back.catchmate.chat.domain.exception.ChatRoomReentryNotAllowedException;
import com.back.catchmate.chat.fixture.ChatFixture;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatRoomMemberTest {

    private static final LocalDateTime NOW = ChatFixture.NOW;

    private static ChatRoomMember member() {
        return ChatRoomMember.create(ChatFixture.room(5L, 10L, 3), 1L, 3L, NOW);
    }

    @Test
    @DisplayName("새 멤버는 활성·알림 켜짐이고 참여 시점의 시퀀스까지 읽은 것으로 시작한다")
    void create() {
        ChatRoomMember member = member();

        assertThat(member.isActive()).isTrue();
        assertThat(member.isNotificationOn()).isTrue();
        assertThat(member.getLastReadSequence()).isEqualTo(3L);
        assertThat(member.getJoinedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("활성 멤버가 다시 들어오면 읽기 전용만 풀린다")
    void rejoinClearsReadOnly() {
        ChatRoomMember member = member();
        member.markAsReadOnly(NOW);

        member.rejoin();

        assertThat(member.isReadOnly()).isFalse();
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("스스로 나간 방에는 다시 들어올 수 없다")
    void rejoinAfterLeaveIsRejected() {
        ChatRoomMember member = member();
        member.leave(NOW);

        assertThatThrownBy(member::rejoin).isInstanceOf(ChatRoomReentryNotAllowedException.class);
    }

    @Test
    @DisplayName("안 읽은 수는 방 시퀀스와의 차이이고 음수가 되지 않는다")
    void unreadCount() {
        ChatRoomMember member = member();

        assertThat(member.calculateUnreadCount(7L)).isEqualTo(4L);
        assertThat(member.calculateUnreadCount(1L)).isZero();
    }

    @Test
    @DisplayName("알림을 끄고 켤 수 있다")
    void notificationToggle() {
        ChatRoomMember member = member();

        member.disableNotification();
        assertThat(member.isNotificationOn()).isFalse();
        member.enableNotification();
        assertThat(member.isNotificationOn()).isTrue();
    }

    @Test
    @DisplayName("멤버십 스냅샷: 퇴장이면 전송·조회 불가, 읽기 전용이면 전송만 불가")
    void snapshotRules() {
        ChatRoomMember readOnly = member();
        readOnly.markAsReadOnly(NOW);
        ChatRoomMember left = member();
        left.leave(NOW);

        readOnly.snapshot().verifyActive();
        assertThatThrownBy(() -> readOnly.snapshot().verifySendable()).isInstanceOf(ChatRoomReadOnlyException.class);
        assertThatThrownBy(() -> left.snapshot().verifyActive()).isInstanceOf(ChatRoomMemberNotFoundException.class);
        assertThatThrownBy(() -> left.snapshot().verifySendable()).isInstanceOf(ChatRoomMemberNotFoundException.class);
    }
}
