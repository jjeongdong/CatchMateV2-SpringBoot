package com.back.catchmate.enroll.domain.model;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrollTest {

    // ── createEnroll ────────────────────────────────────────────────

    @Test
    @DisplayName("신청을 생성하면 대기 상태이면서 새 신청으로 표시된다")
    void 정상_생성이면_PENDING_새신청으로_초기화() {
        // given
        Long applicantId = 1L, boardId = 10L, ownerId = 2L;

        // when
        Enroll enroll = Enroll.createEnroll(applicantId, boardId, ownerId, "직관 같이가요");

        // then
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.PENDING);
        assertThat(enroll.isNewEnroll()).isTrue();
        assertThat(enroll.getRequestedAt()).isNotNull();
        assertThat(enroll.getUserId()).isEqualTo(applicantId);
        assertThat(enroll.getBoardId()).isEqualTo(boardId);
        assertThat(enroll.getBoardOwnerId()).isEqualTo(ownerId);
        assertThat(enroll.getId()).isNull();
    }

    @Test
    @DisplayName("게시글 작성자가 자기 게시글에 신청하면 예외를 던진다")
    void 본인_게시글에_신청하면_예외() {
        // given
        Long ownerId = 2L, boardId = 10L;

        // when & then
        assertThatThrownBy(() -> Enroll.createEnroll(ownerId, boardId, ownerId, "설명"))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ENROLL_BAD_REQUEST));
    }

    // ── accept ──────────────────────────────────────────────────────

    @Test
    @DisplayName("대기 중 신청을 수락하면 수락 상태가 된다")
    void 대기중_신청을_수락하면_ACCEPTED() {
        // given
        Enroll enroll = enroll(AcceptStatus.PENDING, true);

        // when
        enroll.accept();

        // then
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.ACCEPTED);
    }

    @Test
    @DisplayName("이미 수락된 신청을 다시 수락하면 예외를 던지고 상태를 유지한다")
    void 이미_수락된_신청을_다시_수락하면_예외() {
        // given
        Enroll enroll = enroll(AcceptStatus.ACCEPTED, false);

        // when & then
        assertThatThrownBy(enroll::accept)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLL_ACCEPTED));
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.ACCEPTED);
    }

    @Test
    @DisplayName("거절된 신청을 수락하면 수락 상태로 바뀐다")
    void 거절된_신청도_수락할_수_있다() {
        // given
        Enroll enroll = enroll(AcceptStatus.REJECTED, false);

        // when
        enroll.accept();

        // then
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.ACCEPTED);
    }

    // ── reject ──────────────────────────────────────────────────────

    @Test
    @DisplayName("대기 중 신청을 거절하면 거절 상태가 된다")
    void 대기중_신청을_거절하면_REJECTED() {
        // given
        Enroll enroll = enroll(AcceptStatus.PENDING, true);

        // when
        enroll.reject();

        // then
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.REJECTED);
    }

    @Test
    @DisplayName("이미 거절된 신청을 다시 거절하면 예외를 던진다")
    void 이미_거절된_신청을_다시_거절하면_예외() {
        // given
        Enroll enroll = enroll(AcceptStatus.REJECTED, false);

        // when & then
        assertThatThrownBy(enroll::reject)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLL_REJECTED));
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.REJECTED);
    }

    // ── markAsRead ──────────────────────────────────────────────────

    @Test
    @DisplayName("읽음 처리하면 새 신청 표시가 해제된다")
    void 읽음_처리하면_새신청_플래그가_꺼진다() {
        // given
        Enroll enroll = enroll(AcceptStatus.PENDING, true);

        // when
        enroll.markAsRead();

        // then
        assertThat(enroll.isNewEnroll()).isFalse();
    }

    // ── preventNewEnroll ────────────────────────────────────────────

    @Test
    @DisplayName("대기 중 신청이 있으면 재신청 시 대기 중 예외를 던진다")
    void 대기중_신청이_있으면_재신청_차단() {
        // given
        Enroll enroll = enroll(AcceptStatus.PENDING, true);

        // when & then
        assertThatThrownBy(enroll::preventNewEnroll)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLL_PENDING));
    }

    @Test
    @DisplayName("거절된 신청이 있으면 재신청 시 거절 예외를 던진다")
    void 거절된_신청이_있으면_재신청_차단() {
        // given
        Enroll enroll = enroll(AcceptStatus.REJECTED, false);

        // when & then
        assertThatThrownBy(enroll::preventNewEnroll)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLL_REJECTED));
    }

    @Test
    @DisplayName("수락된 신청이 있으면 재신청 시 수락 예외를 던진다")
    void 수락된_신청이_있으면_재신청_차단() {
        // given
        Enroll enroll = enroll(AcceptStatus.ACCEPTED, false);

        // when & then
        assertThatThrownBy(enroll::preventNewEnroll)
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLL_ACCEPTED));
    }

    // ── 테스트 데이터 헬퍼 ──────────────────────────────────────────

    private Enroll enroll(AcceptStatus status, boolean newEnroll) {
        return Enroll.builder()
                .id(100L)
                .userId(1L)
                .boardId(10L)
                .boardOwnerId(2L)
                .description("직관 같이가요")
                .acceptStatus(status)
                .newEnroll(newEnroll)
                .build();
    }
}
