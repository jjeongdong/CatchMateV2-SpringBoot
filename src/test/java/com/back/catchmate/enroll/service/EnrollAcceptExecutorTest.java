package com.back.catchmate.enroll.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.enroll.dto.response.EnrollAcceptResponse;
import com.back.catchmate.enroll.entity.AcceptStatus;
import com.back.catchmate.enroll.entity.Enroll;
import com.back.catchmate.enroll.event.EnrollAcceptedEvent;
import com.back.catchmate.enroll.repository.EnrollRepository;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class EnrollAcceptExecutorTest {

    @Mock
    private EnrollRepository enrollRepository;

    @Mock
    private BoardService boardService; // cross-context: 자기 FetchPort 를 모킹

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private EnrollAcceptExecutor sut;

    // ── accept ──────────────────────────────────────────────────────

    @Test
    @DisplayName("게시글 호스트가 수락하면 상태를 ACCEPTED 로 바꾸고 수락 이벤트를 발행한다")
    void 호스트가_수락하면_상태변경_저장_수락이벤트발행() {
        // given
        Long ownerId = 2L, applicantId = 1L, boardId = 10L, enrollId = 100L;
        Enroll enroll = enroll(enrollId, applicantId, boardId, ownerId, AcceptStatus.PENDING, true);
        given(enrollRepository.findById(enrollId)).willReturn(Optional.of(enroll));
        given(boardService.getBoardSummary(boardId)).willReturn(boardInfo(boardId, ownerId));
        given(userQueryApi.getInfo(applicantId)).willReturn(userInfo(applicantId));

        // when
        EnrollAcceptResponse response = sut.accept(ownerId, enrollId);

        // then
        assertThat(response.enrollId()).isEqualTo(enrollId);
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.ACCEPTED);
        then(enrollRepository).should().save(enroll);

        ArgumentCaptor<EnrollAcceptedEvent> eventCaptor = ArgumentCaptor.forClass(EnrollAcceptedEvent.class);
        then(applicationEventPublisher).should().publishEvent(eventCaptor.capture());
        EnrollAcceptedEvent published = eventCaptor.getValue();
        assertThat(published.enrollId()).isEqualTo(enrollId);
        assertThat(published.boardId()).isEqualTo(boardId);
        assertThat(published.applicantId()).isEqualTo(applicantId);
        assertThat(published.boardOwnerId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("게시글 호스트가 아닌 사용자가 수락하면 권한 예외를 던지고 아무 조회도 하지 않는다")
    void 호스트가_아니면_수락_시_권한예외_외부조회없음() {
        // given
        Long ownerId = 2L, otherUserId = 999L, enrollId = 100L;
        Enroll enroll = enroll(enrollId, 1L, 10L, ownerId, AcceptStatus.PENDING, true);
        given(enrollRepository.findById(enrollId)).willReturn(Optional.of(enroll));

        // when & then
        assertThatThrownBy(() -> sut.accept(otherUserId, enrollId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_ACCESS));
        then(boardService).shouldHaveNoInteractions();
        then(userQueryApi).shouldHaveNoInteractions();
        then(enrollRepository).should(never()).save(any());
        then(applicationEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 수락된 신청을 수락하면 예외를 던지고 저장·이벤트가 일어나지 않는다")
    void 이미_수락된_신청을_수락하면_저장도_이벤트도_없다() {
        // given
        Long ownerId = 2L, applicantId = 1L, boardId = 10L, enrollId = 100L;
        Enroll enroll = enroll(enrollId, applicantId, boardId, ownerId, AcceptStatus.ACCEPTED, false);
        given(enrollRepository.findById(enrollId)).willReturn(Optional.of(enroll));
        given(boardService.getBoardSummary(boardId)).willReturn(boardInfo(boardId, ownerId));
        given(userQueryApi.getInfo(applicantId)).willReturn(userInfo(applicantId));

        // when & then
        assertThatThrownBy(() -> sut.accept(ownerId, enrollId))
                .isInstanceOf(BaseException.class)
                .satisfies(e ->
                        assertThat(((BaseException) e).getErrorCode()).isEqualTo(ErrorCode.ALREADY_ENROLL_ACCEPTED));
        then(enrollRepository).should(never()).save(any());
        then(applicationEventPublisher).shouldHaveNoInteractions();
    }

    // ── recover (재시도 소진) ───────────────────────────────────────

    @Test
    @DisplayName("낙관적 락 재시도가 모두 실패하면 수락 충돌 예외로 변환한다")
    void 재시도_소진되면_수락_충돌_예외로_변환() {
        // given
        ObjectOptimisticLockingFailureException lockFailure =
                new ObjectOptimisticLockingFailureException(Object.class, 1L);

        // when & then
        assertThatThrownBy(() -> sut.recover(lockFailure, 2L, 100L))
                .isInstanceOf(BaseException.class)
                .satisfies(e ->
                        assertThat(((BaseException) e).getErrorCode()).isEqualTo(ErrorCode.ENROLL_ACCEPT_CONFLICT));
        then(enrollRepository).shouldHaveNoInteractions();
        then(applicationEventPublisher).shouldHaveNoInteractions();
    }

    // ── 테스트 데이터 헬퍼 ──────────────────────────────────────────

    private Enroll enroll(
            Long id, Long userId, Long boardId, Long boardOwnerId, AcceptStatus status, boolean newEnroll) {
        return Enroll.builder()
                .id(id)
                .userId(userId)
                .boardId(boardId)
                .boardOwnerId(boardOwnerId)
                .description("직관 같이가요")
                .acceptStatus(status)
                .newEnroll(newEnroll)
                .build();
    }

    private UserInfo userInfo(Long userId) {
        return new UserInfo(
                userId,
                "test@catchmate.com",
                null,
                null,
                'M',
                "홍길동",
                null,
                null,
                null,
                "USER",
                null,
                1L,
                false,
                false,
                false,
                false,
                null,
                null);
    }

    private BoardSummary boardInfo(Long boardId, Long ownerId) {
        return new BoardSummary(boardId, "제목", "내용", 4, 1, ownerId, 1L, 1L, null, null, false, null, null);
    }
}
