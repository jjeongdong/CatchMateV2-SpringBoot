package com.back.catchmate.enroll.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.fixture.BoardFixture;
import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;
import com.back.catchmate.enroll.application.dto.result.EnrollAcceptResult;
import com.back.catchmate.enroll.application.dto.result.EnrollCreateResult;
import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.enroll.domain.EnrollAcceptIdempotencyStore;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.domain.event.EnrollCancelledEvent;
import com.back.catchmate.enroll.domain.event.EnrollRejectedEvent;
import com.back.catchmate.enroll.domain.event.EnrollRequestedEvent;
import com.back.catchmate.enroll.domain.exception.EnrollAcceptInProgressException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyPendingException;
import com.back.catchmate.enroll.domain.exception.EnrollNotApplicantException;
import com.back.catchmate.enroll.domain.exception.EnrollNotBoardWriterException;
import com.back.catchmate.enroll.fixture.EnrollFixture;
import com.back.catchmate.user.application.UserQueryApi;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EnrollCommandServiceTest {

    private static final Long APPLICANT = 1L;
    private static final Long WRITER = 2L;
    private static final Long BOARD = 10L;

    @Mock
    private EnrollRepository enrollRepository;

    @Mock
    private EnrollAcceptIdempotencyStore idempotencyStore;

    @Mock
    private EnrollAcceptExecutor enrollAcceptExecutor;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private BoardQueryApi boardQueryApi;

    @InjectMocks
    private EnrollCommandService enrollCommandService;

    @Test
    @DisplayName("신청하면 저장하고 게시글 작성자를 담아 신청 이벤트를 낸다")
    void createEnroll() {
        // given
        given(boardQueryApi.getPublishedInfo(BOARD)).willReturn(BoardFixture.info(BOARD, WRITER));
        given(enrollRepository.findByApplicantIdAndBoardId(APPLICANT, BOARD)).willReturn(Optional.empty());
        willAnswer(invocation -> {
                    Enroll enroll = invocation.getArgument(0);
                    ReflectionTestUtils.setField(enroll, "id", 100L);
                    return enroll;
                })
                .given(enrollRepository)
                .save(any(Enroll.class));

        // when
        EnrollCreateResult result =
                enrollCommandService.createEnroll(APPLICANT, BOARD, new EnrollCreateCommand("같이 가요"));

        // then
        assertThat(result.enrollId()).isEqualTo(100L);
        then(userQueryApi).should().getInfo(APPLICANT);
        then(eventPublisher).should().publishEvent(new EnrollRequestedEvent(100L, BOARD, APPLICANT, WRITER));
    }

    @Test
    @DisplayName("이미 대기 중인 신청이 있으면 저장·이벤트 없이 예외")
    void createEnrollRejectsDuplicate() {
        // given
        given(boardQueryApi.getPublishedInfo(BOARD)).willReturn(BoardFixture.info(BOARD, WRITER));
        given(enrollRepository.findByApplicantIdAndBoardId(APPLICANT, BOARD))
                .willReturn(Optional.of(EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER)));

        // when & then
        assertThatThrownBy(() -> enrollCommandService.createEnroll(APPLICANT, BOARD, new EnrollCreateCommand("d")))
                .isInstanceOf(EnrollAlreadyPendingException.class);
        then(enrollRepository).should(never()).save(any());
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("작성자가 거절하면 게시글 조회 없이 스냅샷 값으로 거절 이벤트를 낸다")
    void rejectEnroll() {
        // given
        Enroll enroll = EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER);
        given(enrollRepository.getById(100L)).willReturn(enroll);

        // when
        enrollCommandService.rejectEnroll(WRITER, 100L);

        // then
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.REJECTED);
        then(eventPublisher).should().publishEvent(new EnrollRejectedEvent(100L, BOARD, APPLICANT, WRITER));
        then(boardQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("작성자가 아니면 거절할 수 없고 이벤트도 없다")
    void rejectEnrollByOther() {
        given(enrollRepository.getById(100L)).willReturn(EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER));

        assertThatThrownBy(() -> enrollCommandService.rejectEnroll(APPLICANT, 100L))
                .isInstanceOf(EnrollNotBoardWriterException.class);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("신청자가 취소하면 지우고 취소 이벤트를 낸다. 남은 취소할 수 없다")
    void deleteEnroll() {
        // given
        Enroll enroll = EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER);
        given(enrollRepository.getById(100L)).willReturn(enroll);

        // when & then
        assertThatThrownBy(() -> enrollCommandService.deleteEnroll(WRITER, 100L))
                .isInstanceOf(EnrollNotApplicantException.class);
        enrollCommandService.deleteEnroll(APPLICANT, 100L);
        then(enrollRepository).should().delete(enroll);
        then(eventPublisher).should().publishEvent(new EnrollCancelledEvent(100L, BOARD, APPLICANT, WRITER));
    }

    @Test
    @DisplayName("멱등성 키를 선점하지 못하면 수락을 위임하지 않는다")
    void acceptInProgress() {
        given(idempotencyStore.acquire(100L)).willReturn(false);

        assertThatThrownBy(() -> enrollCommandService.acceptEnroll(WRITER, 100L))
                .isInstanceOf(EnrollAcceptInProgressException.class);
        then(enrollAcceptExecutor).shouldHaveNoInteractions();
        then(idempotencyStore).should(never()).release(any());
    }

    @Test
    @DisplayName("수락을 위임하고 성공하든 실패하든 키를 해제한다")
    void acceptReleasesKey() {
        // given
        given(idempotencyStore.acquire(100L)).willReturn(true);
        given(enrollAcceptExecutor.accept(WRITER, 100L)).willReturn(EnrollAcceptResult.of(100L));
        given(enrollAcceptExecutor.accept(WRITER, 101L)).willThrow(new EnrollNotBoardWriterException());
        given(idempotencyStore.acquire(101L)).willReturn(true);

        // when
        EnrollAcceptResult result = enrollCommandService.acceptEnroll(WRITER, 100L);

        // then
        assertThat(result.enrollId()).isEqualTo(100L);
        then(idempotencyStore).should().release(100L);
        assertThatThrownBy(() -> enrollCommandService.acceptEnroll(WRITER, 101L))
                .isInstanceOf(EnrollNotBoardWriterException.class);
        then(idempotencyStore).should().release(101L);
    }

    @Test
    @DisplayName("차단하면 차단자가 차단 대상의 글에 낸 수락된 신청을 지운다")
    void deleteAcceptedEnrollsBetween() {
        // given
        Enroll accepted = EnrollFixture.pending(100L, 1L, BOARD, 2L);
        given(enrollRepository.findAcceptedByApplicantIdAndOwnerId(1L, 2L)).willReturn(List.of(accepted));

        // when
        enrollCommandService.deleteAcceptedEnrollsBetween(1L, 2L);

        // then
        then(enrollRepository).should().delete(accepted);
    }

    @Test
    @DisplayName("작성자가 읽음 처리하면 새 신청 표시가 꺼진다")
    void markEnrollAsRead() {
        Enroll enroll = EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER);
        given(enrollRepository.getById(100L)).willReturn(enroll);

        enrollCommandService.markEnrollAsRead(WRITER, 100L);

        assertThat(enroll.isNewEnroll()).isFalse();
    }
}
