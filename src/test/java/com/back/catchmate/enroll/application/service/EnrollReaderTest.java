package com.back.catchmate.enroll.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.enroll.application.port.out.persistence.EnrollRepository;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class EnrollReaderTest {

    @Mock
    private EnrollRepository enrollRepository;

    @InjectMocks
    private EnrollReader sut;

    // ── getEnroll ───────────────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 신청을 조회하면 신청 없음 예외를 던진다")
    void 존재하지_않는_신청을_조회하면_예외() {
        // given
        given(enrollRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.getEnroll(999L))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ENROLL_NOT_FOUND));
    }

    // ── checkDuplicateEnroll ────────────────────────────────────────

    @Test
    @DisplayName("동일 게시글에 기존 신청이 없으면 중복 검사를 통과한다")
    void 기존_신청이_없으면_중복검사를_통과한다() {
        // given
        Long applicantId = 1L, boardId = 10L;
        given(enrollRepository.findByUserIdAndBoardId(applicantId, boardId)).willReturn(Optional.empty());

        // when & then
        assertThatCode(() -> sut.checkDuplicateEnroll(applicantId, boardId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동일 게시글에 대기 중 신청이 있으면 중복 신청 예외가 전파된다")
    void 기존_대기중_신청이_있으면_도메인_예외가_전파된다() {
        // given
        Long applicantId = 1L, boardId = 10L, ownerId = 2L;
        given(enrollRepository.findByUserIdAndBoardId(applicantId, boardId))
                .willReturn(Optional.of(enroll(100L, applicantId, boardId, ownerId, AcceptStatus.PENDING, true)));

        // when & then
        assertThatThrownBy(() -> sut.checkDuplicateEnroll(applicantId, boardId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_ENROLL_PENDING));
    }

    // ── getAcceptedEnrollsBetween ───────────────────────────────────

    @Test
    @DisplayName("차단된 사용자와의 신청을 조회할 때 수락 상태만 조회한다")
    void 차단_시_삭제대상은_수락된_신청만_조회한다() {
        // given
        Long applicantId = 1L, ownerId = 2L;
        given(enrollRepository.findAllByApplicantAndBoardOwnerAndStatus(applicantId, ownerId, AcceptStatus.ACCEPTED))
                .willReturn(List.of(enroll(100L, applicantId, 10L, ownerId, AcceptStatus.ACCEPTED, false)));

        // when
        List<Enroll> result = sut.getAcceptedEnrollsBetween(applicantId, ownerId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        then(enrollRepository).should()
                .findAllByApplicantAndBoardOwnerAndStatus(applicantId, ownerId, AcceptStatus.ACCEPTED);
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
}
