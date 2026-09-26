package com.back.catchmate.enroll.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.enroll.domain.exception.EnrollAlreadyAcceptedException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyPendingException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyRejectedException;
import com.back.catchmate.enroll.domain.exception.EnrollNotApplicantException;
import com.back.catchmate.enroll.domain.exception.EnrollNotBoardWriterException;
import com.back.catchmate.enroll.domain.exception.EnrollNotParticipantException;
import com.back.catchmate.enroll.domain.exception.EnrollSelfNotAllowedException;
import com.back.catchmate.enroll.fixture.EnrollFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnrollTest {

    private static final Long APPLICANT = 1L;
    private static final Long WRITER = 2L;
    private static final Long STRANGER = 3L;

    private static Enroll pending() {
        return EnrollFixture.pending(100L, APPLICANT, 10L, WRITER);
    }

    @Test
    @DisplayName("신청하면 대기 상태의 새 신청이다")
    void create() {
        Enroll enroll = Enroll.create(APPLICANT, 10L, WRITER, "같이 가요");

        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.PENDING);
        assertThat(enroll.isNewEnroll()).isTrue();
        assertThat(enroll.getBoardOwnerId()).isEqualTo(WRITER);
    }

    @Test
    @DisplayName("자기 게시글에는 신청할 수 없다")
    void createRejectsOwnBoard() {
        assertThatThrownBy(() -> Enroll.create(WRITER, 10L, WRITER, "d"))
                .isInstanceOf(EnrollSelfNotAllowedException.class);
    }

    @Test
    @DisplayName("작성자만 수락할 수 있고, 이미 수락했으면 다시 수락할 수 없다")
    void accept() {
        // given
        Enroll enroll = pending();

        // when & then
        assertThatThrownBy(() -> enroll.accept(APPLICANT)).isInstanceOf(EnrollNotBoardWriterException.class);
        enroll.accept(WRITER);
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.ACCEPTED);
        assertThatThrownBy(() -> enroll.accept(WRITER)).isInstanceOf(EnrollAlreadyAcceptedException.class);
    }

    @Test
    @DisplayName("거절된 신청도 수락할 수 있다 (옛 동작)")
    void acceptAfterReject() {
        Enroll enroll = pending();
        enroll.reject(WRITER);

        enroll.accept(WRITER);

        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.ACCEPTED);
    }

    @Test
    @DisplayName("작성자만 거절할 수 있고, 이미 거절했으면 다시 거절할 수 없다")
    void reject() {
        Enroll enroll = pending();

        assertThatThrownBy(() -> enroll.reject(STRANGER)).isInstanceOf(EnrollNotBoardWriterException.class);
        enroll.reject(WRITER);
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.REJECTED);
        assertThatThrownBy(() -> enroll.reject(WRITER)).isInstanceOf(EnrollAlreadyRejectedException.class);
    }

    @Test
    @DisplayName("작성자가 읽으면 새 신청 표시가 꺼진다")
    void markAsRead() {
        Enroll enroll = pending();

        assertThatThrownBy(() -> enroll.markAsRead(APPLICANT)).isInstanceOf(EnrollNotBoardWriterException.class);
        enroll.markAsRead(WRITER);
        assertThat(enroll.isNewEnroll()).isFalse();
    }

    @Test
    @DisplayName("상태별로 재신청을 막는다")
    void preventReapply() {
        Enroll pending = pending();
        Enroll rejected = pending();
        rejected.reject(WRITER);
        Enroll accepted = pending();
        accepted.accept(WRITER);

        assertThatThrownBy(pending::preventReapply).isInstanceOf(EnrollAlreadyPendingException.class);
        assertThatThrownBy(rejected::preventReapply).isInstanceOf(EnrollAlreadyRejectedException.class);
        assertThatThrownBy(accepted::preventReapply).isInstanceOf(EnrollAlreadyAcceptedException.class);
    }

    @Test
    @DisplayName("신청자만 취소할 수 있고, 신청자·작성자만 상세를 볼 수 있다")
    void verifyApplicantAndParticipant() {
        Enroll enroll = pending();

        assertThatCode(() -> enroll.verifyApplicant(APPLICANT)).doesNotThrowAnyException();
        assertThatThrownBy(() -> enroll.verifyApplicant(WRITER)).isInstanceOf(EnrollNotApplicantException.class);
        assertThatCode(() -> enroll.verifyParticipant(WRITER)).doesNotThrowAnyException();
        assertThatThrownBy(() -> enroll.verifyParticipant(STRANGER)).isInstanceOf(EnrollNotParticipantException.class);
    }
}
