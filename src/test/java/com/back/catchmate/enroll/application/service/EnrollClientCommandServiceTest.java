package com.back.catchmate.enroll.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;
import com.back.catchmate.enroll.application.dto.response.EnrollAcceptResponse;
import com.back.catchmate.enroll.application.event.EnrollRejectedEvent;
import com.back.catchmate.enroll.application.event.EnrollRequestedEvent;
import com.back.catchmate.enroll.application.port.out.dto.EnrollBoardInfo;
import com.back.catchmate.enroll.application.port.out.dto.EnrollUserInfo;
import com.back.catchmate.enroll.application.port.out.external.BoardFetchPort;
import com.back.catchmate.enroll.application.port.out.external.IdempotencyPort;
import com.back.catchmate.enroll.application.port.out.external.UserFetchPort;
import com.back.catchmate.enroll.application.port.out.persistence.EnrollRepository;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class EnrollClientCommandServiceTest {

    @Mock
    private EnrollRepository enrollRepository;
    @Mock
    private EnrollReader enrollReader;
    @Mock
    private UserFetchPort userFetchPort;                 // cross-context: 자기 FetchPort 를 모킹
    @Mock
    private BoardFetchPort boardFetchPort;
    @Mock
    private IdempotencyPort idempotencyPort;
    @Mock
    private EnrollAcceptExecutor enrollAcceptExecutor;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private EnrollClientCommandService sut;

    // ── createEnroll ────────────────────────────────────────────────

    @Test
    @DisplayName("신청 생성 시 저장하고 신청 요청 이벤트를 발행한다")
    void 신청_생성_시_저장하고_요청_이벤트를_발행한다() {
        // given
        Long applicantId = 1L, boardId = 10L, ownerId = 2L;
        EnrollCreateCommand command = new EnrollCreateCommand(applicantId, boardId, "직관 같이가요");
        given(userFetchPort.getUser(applicantId)).willReturn(userInfo(applicantId));
        given(boardFetchPort.getCompletedBoard(boardId)).willReturn(boardInfo(boardId, ownerId));
        given(enrollRepository.save(any(Enroll.class)))
                .willReturn(enroll(100L, applicantId, boardId, ownerId, AcceptStatus.PENDING, true));

        // when
        var response = sut.createEnroll(command);

        // then
        assertThat(response.enrollId()).isEqualTo(100L);
        then(enrollReader).should().checkDuplicateEnroll(applicantId, boardId);
        then(enrollRepository).should().save(any(Enroll.class));
        then(applicationEventPublisher).should().publishEvent(any(EnrollRequestedEvent.class));
    }

    @Test
    @DisplayName("중복 신청이면 저장·이벤트 없이 예외를 던진다")
    void 중복_신청이면_저장과_이벤트_없이_예외() {
        // given
        Long applicantId = 1L, boardId = 10L, ownerId = 2L;
        EnrollCreateCommand command = new EnrollCreateCommand(applicantId, boardId, "직관 같이가요");
        given(userFetchPort.getUser(applicantId)).willReturn(userInfo(applicantId));
        given(boardFetchPort.getCompletedBoard(boardId)).willReturn(boardInfo(boardId, ownerId));
        willThrow(new BaseException(ErrorCode.ALREADY_ENROLL_PENDING))
                .given(enrollReader).checkDuplicateEnroll(applicantId, boardId);

        // when & then
        assertThatThrownBy(() -> sut.createEnroll(command))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLL_PENDING));
        then(enrollRepository).should(never()).save(any());
        then(applicationEventPublisher).shouldHaveNoInteractions();
    }

    // ── updateEnrollReject ──────────────────────────────────────────

    @Test
    @DisplayName("게시글 호스트가 신청을 거절하면 상태를 REJECTED 로 바꾸고 거절 이벤트를 발행한다")
    void 호스트가_거절하면_상태변경하고_거절_이벤트_발행() {
        // given
        Long ownerId = 2L, applicantId = 1L, boardId = 10L, enrollId = 100L;
        Enroll enroll = enroll(enrollId, applicantId, boardId, ownerId, AcceptStatus.PENDING, true);
        given(enrollReader.getEnroll(enrollId)).willReturn(enroll);
        given(boardFetchPort.getBoard(boardId)).willReturn(boardInfo(boardId, ownerId));
        given(userFetchPort.getUser(applicantId)).willReturn(userInfo(applicantId));

        // when
        var response = sut.updateEnrollReject(ownerId, enrollId);

        // then
        assertThat(response.enrollId()).isEqualTo(enrollId);
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.REJECTED);
        then(enrollRepository).should().save(enroll);
        then(applicationEventPublisher).should().publishEvent(any(EnrollRejectedEvent.class));
    }

    @Test
    @DisplayName("호스트가 아닌 사용자가 거절하면 권한 예외를 던지고 아무 것도 변경하지 않는다")
    void 호스트가_아니면_거절_시_권한예외() {
        // given
        Long ownerId = 2L, otherUserId = 999L, enrollId = 100L;
        Enroll enroll = enroll(enrollId, 1L, 10L, ownerId, AcceptStatus.PENDING, true);
        given(enrollReader.getEnroll(enrollId)).willReturn(enroll);

        // when & then
        assertThatThrownBy(() -> sut.updateEnrollReject(otherUserId, enrollId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN_ACCESS));
        then(enrollRepository).should(never()).save(any());
        then(applicationEventPublisher).shouldHaveNoInteractions();
        then(boardFetchPort).shouldHaveNoInteractions();
    }

    // ── deleteEnroll ────────────────────────────────────────────────

    @Test
    @DisplayName("신청자 본인이 아니면 취소 시 권한 예외를 던진다")
    void 신청자_본인이_아니면_취소_시_권한예외() {
        // given
        Long applicantId = 1L, otherUserId = 999L, enrollId = 100L;
        Enroll enroll = enroll(enrollId, applicantId, 10L, 2L, AcceptStatus.PENDING, true);
        given(enrollReader.getEnroll(enrollId)).willReturn(enroll);

        // when & then
        assertThatThrownBy(() -> sut.deleteEnroll(otherUserId, enrollId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN_ACCESS));
        then(enrollRepository).should(never()).delete(any());
        then(applicationEventPublisher).shouldHaveNoInteractions();
    }

    // ── updateEnrollAccept (멱등성 경계) ────────────────────────────

    @Test
    @DisplayName("멱등성 키 선점에 실패하면 중복 수락 요청 예외를 던지고 실행기에 위임하지 않는다")
    void 멱등성_선점_실패면_중복수락_예외_위임없음() {
        // given
        given(idempotencyPort.acquireIfAbsent(anyString(), anyLong())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.updateEnrollAccept(2L, 100L))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_ENROLL_ACCEPT_REQUEST));
        then(enrollAcceptExecutor).shouldHaveNoInteractions();
        then(idempotencyPort).should(never()).release(any());
    }

    @Test
    @DisplayName("멱등성 키를 선점하면 실제 수락 처리를 실행기에 위임한다")
    void 멱등성_선점_성공이면_실행기에_위임() {
        // given
        Long userId = 2L, enrollId = 100L;
        given(idempotencyPort.acquireIfAbsent(anyString(), anyLong())).willReturn(true);
        given(enrollAcceptExecutor.accept(userId, enrollId))
                .willReturn(EnrollAcceptResponse.of(enrollId));

        // when
        var response = sut.updateEnrollAccept(userId, enrollId);

        // then
        assertThat(response.enrollId()).isEqualTo(enrollId);
        then(enrollAcceptExecutor).should().accept(userId, enrollId);
    }

    @Test
    @DisplayName("수락에 성공하면 멱등성 키를 즉시 해제한다")
    void 수락_성공하면_멱등성_키를_즉시_해제() {
        // given
        Long userId = 2L, enrollId = 100L;
        String idempotencyKey = "idempotent:enroll:accept:" + enrollId;
        given(idempotencyPort.acquireIfAbsent(anyString(), anyLong())).willReturn(true);
        given(enrollAcceptExecutor.accept(userId, enrollId))
                .willReturn(EnrollAcceptResponse.of(enrollId));

        // when
        sut.updateEnrollAccept(userId, enrollId);

        // then
        then(idempotencyPort).should().release(idempotencyKey);
    }

    @Test
    @DisplayName("실행기가 예외를 던져도 멱등성 키를 해제해 재시도 안내 후 다시 시도할 수 있게 한다")
    void 실행기_예외여도_멱등성_키를_해제() {
        // given
        Long userId = 2L, enrollId = 100L;
        String idempotencyKey = "idempotent:enroll:accept:" + enrollId;
        given(idempotencyPort.acquireIfAbsent(anyString(), anyLong())).willReturn(true);
        willThrow(new BaseException(ErrorCode.ENROLL_ACCEPT_CONFLICT))
                .given(enrollAcceptExecutor).accept(userId, enrollId);

        // when & then
        assertThatThrownBy(() -> sut.updateEnrollAccept(userId, enrollId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ENROLL_ACCEPT_CONFLICT));
        then(idempotencyPort).should().release(idempotencyKey);
    }

    // ── 테스트 데이터 헬퍼 ──────────────────────────────────────────

    private Enroll enroll(Long id, Long userId, Long boardId, Long boardOwnerId,
                          AcceptStatus status, boolean newEnroll) {
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

    private EnrollUserInfo userInfo(Long userId) {
        return new EnrollUserInfo(userId, 1L, "홍길동", "test@catchmate.com",
                null, 'M', null, null, "USER");
    }

    private EnrollBoardInfo boardInfo(Long boardId, Long ownerId) {
        return new EnrollBoardInfo(boardId, ownerId, "제목", "내용", 1, 4, 1L, 1L);
    }
}
