package com.back.catchmate.enroll.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollApplicantResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollDetailResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollReceiveResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollRequestResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollResponse;
import com.back.catchmate.enroll.application.port.out.dto.EnrollBoardInfo;
import com.back.catchmate.enroll.application.port.out.dto.EnrollClubInfo;
import com.back.catchmate.enroll.application.port.out.dto.EnrollUserInfo;
import com.back.catchmate.enroll.application.port.out.external.BoardFetchPort;
import com.back.catchmate.enroll.application.port.out.external.BookmarkFetchPort;
import com.back.catchmate.enroll.application.port.out.external.ClubFetchPort;
import com.back.catchmate.enroll.application.port.out.external.GameFetchPort;
import com.back.catchmate.enroll.application.port.out.external.UserFetchPort;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class EnrollClientQueryServiceTest {

    @Mock
    private EnrollReader enrollReader;
    @Mock
    private BoardFetchPort boardFetchPort;                // cross-context: 자기 FetchPort 를 모킹
    @Mock
    private BookmarkFetchPort bookmarkFetchPort;
    @Mock
    private ClubFetchPort clubFetchPort;
    @Mock
    private GameFetchPort gameFetchPort;
    @Mock
    private UserFetchPort userFetchPort;

    @InjectMocks
    private EnrollClientQueryService sut;

    // ── getEnroll ───────────────────────────────────────────────────

    @Test
    @DisplayName("신청자도 작성자도 아닌 사용자가 신청 상세를 조회하면 권한 예외를 던진다")
    void 제3자가_신청상세를_조회하면_권한예외() {
        // given
        Long enrollId = 100L, applicantId = 1L, boardId = 10L, writerId = 2L, otherUserId = 999L;
        given(enrollReader.getEnroll(enrollId))
                .willReturn(enroll(enrollId, applicantId, boardId, writerId, AcceptStatus.PENDING, true));
        given(boardFetchPort.getBoard(boardId)).willReturn(board(boardId, writerId));

        // when & then
        assertThatThrownBy(() -> sut.getEnroll(otherUserId, enrollId))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN_ACCESS));
        then(userFetchPort).shouldHaveNoInteractions();
        then(clubFetchPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("신청자 본인이 조회하면 상세를 반환하고 소속 구단이 없으면 구단을 조회하지 않는다")
    void 신청자_본인이_조회하면_상세를_반환하고_소속구단이_없으면_구단조회를_생략() {
        // given
        Long enrollId = 100L, applicantId = 1L, boardId = 10L, writerId = 2L;
        given(enrollReader.getEnroll(enrollId))
                .willReturn(enroll(enrollId, applicantId, boardId, writerId, AcceptStatus.PENDING, true));
        given(boardFetchPort.getBoard(boardId)).willReturn(board(boardId, writerId));
        given(userFetchPort.getUser(applicantId)).willReturn(user(applicantId, null));
        given(userFetchPort.getUsers(List.of(writerId))).willReturn(List.of(user(writerId, null)));

        // when
        EnrollDetailResponse response = sut.getEnroll(applicantId, enrollId);

        // then
        assertThat(response.enrollId()).isEqualTo(enrollId);
        assertThat(response.acceptStatus()).isEqualTo(AcceptStatus.PENDING);
        assertThat(response.applicant().userId()).isEqualTo(applicantId);
        assertThat(response.applicant().club()).isNull();
        assertThat(response.boardResponse().userResponse().userId()).isEqualTo(writerId);
        assertThat(response.boardResponse().bookMarked()).isFalse();
        then(clubFetchPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("게시글 작성자가 조회하면 신청자의 소속 구단까지 채워 반환한다")
    void 게시글_작성자가_조회하면_신청자_소속구단까지_채워서_반환() {
        // given
        Long enrollId = 100L, applicantId = 1L, boardId = 10L, writerId = 2L, clubId = 5L;
        given(enrollReader.getEnroll(enrollId))
                .willReturn(enroll(enrollId, applicantId, boardId, writerId, AcceptStatus.PENDING, true));
        given(boardFetchPort.getBoard(boardId)).willReturn(board(boardId, writerId));
        given(userFetchPort.getUser(applicantId)).willReturn(user(applicantId, clubId));
        given(userFetchPort.getUsers(List.of(writerId))).willReturn(List.of(user(writerId, null)));
        given(clubFetchPort.getClub(clubId)).willReturn(club(clubId));

        // when
        EnrollDetailResponse response = sut.getEnroll(writerId, enrollId);

        // then
        assertThat(response.applicant().club().clubId()).isEqualTo(clubId);
        assertThat(response.applicant().club().name()).isEqualTo("LG 트윈스");
        then(clubFetchPort).should().getClub(clubId);
    }

    // ── getEnrollRequestList ────────────────────────────────────────

    @Test
    @DisplayName("보낸 신청이 없으면 게시글·북마크를 조회하지 않고 빈 목록을 반환한다")
    void 보낸_신청이_없으면_외부포트를_호출하지_않는다() {
        // given
        Long userId = 1L;
        given(enrollReader.getEnrollListByUserId(eq(userId), any(Pageable.class)))
                .willReturn(Page.empty(PageRequest.of(0, 10)));

        // when
        PagedResponse<EnrollRequestResponse> response = sut.getEnrollRequestList(userId, 0, 10);

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        then(bookmarkFetchPort).shouldHaveNoInteractions();
        then(boardFetchPort).shouldHaveNoInteractions();
        then(userFetchPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 게시글에 대한 신청이 여러 건이어도 게시글을 한 번만 조회하고 북마크 여부를 반영한다")
    void 같은_게시글_신청이_여러건이면_게시글을_한_번만_조회하고_북마크를_반영() {
        // given
        Long userId = 1L, boardId = 10L, writerId = 2L;
        given(enrollReader.getEnrollListByUserId(eq(userId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        enroll(100L, userId, boardId, writerId, AcceptStatus.PENDING, true),
                        enroll(101L, userId, boardId, writerId, AcceptStatus.REJECTED, false)
                ), PageRequest.of(0, 10), 2));
        given(bookmarkFetchPort.findBookmarkedBoardIds(userId, List.of(boardId))).willReturn(Set.of(boardId));
        given(boardFetchPort.getBoards(List.of(boardId))).willReturn(List.of(board(boardId, writerId)));
        given(userFetchPort.getUsers(List.of(writerId))).willReturn(List.of(user(writerId, null)));

        // when
        PagedResponse<EnrollRequestResponse> response = sut.getEnrollRequestList(userId, 0, 10);

        // then
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent())
                .allSatisfy(item -> {
                    assertThat(item.boardResponse().boardId()).isEqualTo(boardId);
                    assertThat(item.boardResponse().bookMarked()).isTrue();
                });
        assertThat(response.getContent().get(0).acceptStatus()).isEqualTo(AcceptStatus.PENDING);
        assertThat(response.getContent().get(1).acceptStatus()).isEqualTo(AcceptStatus.REJECTED);
        then(boardFetchPort).should(times(1)).getBoards(List.of(boardId));
    }

    // ── getEnrollReceiveListByBoardId ───────────────────────────────

    @Test
    @DisplayName("내 게시글이 아닌 게시글의 신청자 목록을 조회하면 권한 예외를 던진다")
    void 내_게시글이_아닌_신청자목록을_조회하면_권한예외() {
        // given
        Long boardId = 10L, writerId = 2L, otherUserId = 999L;
        given(boardFetchPort.getBoard(boardId)).willReturn(board(boardId, writerId));

        // when & then
        assertThatThrownBy(() -> sut.getEnrollReceiveListByBoardId(otherUserId, boardId, 0, 10))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN_ACCESS));
        then(enrollReader).shouldHaveNoInteractions();
        then(userFetchPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("내 게시글의 대기 중 신청자 목록을 소속 구단과 함께 반환한다")
    void 내_게시글의_대기중_신청자를_소속구단과_함께_반환() {
        // given
        Long boardId = 10L, writerId = 2L, applicantId = 1L, clubId = 5L;
        given(boardFetchPort.getBoard(boardId)).willReturn(board(boardId, writerId));
        given(enrollReader.getEnrollListByBoardIdAndStatus(eq(boardId), eq(AcceptStatus.PENDING), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        enroll(100L, applicantId, boardId, writerId, AcceptStatus.PENDING, true)
                ), PageRequest.of(0, 10), 1));
        given(userFetchPort.getUsers(List.of(applicantId))).willReturn(List.of(user(applicantId, clubId)));
        given(clubFetchPort.getClubs(List.of(clubId))).willReturn(List.of(club(clubId)));

        // when
        PagedResponse<EnrollApplicantResponse> response = sut.getEnrollReceiveListByBoardId(writerId, boardId, 0, 10);

        // then
        assertThat(response.getContent()).hasSize(1);
        EnrollApplicantResponse applicant = response.getContent().get(0);
        assertThat(applicant.enrollId()).isEqualTo(100L);
        assertThat(applicant.applicantResponse().userId()).isEqualTo(applicantId);
        assertThat(applicant.applicantResponse().favoriteClub()).isEqualTo("LG 트윈스");
        then(enrollReader).should()
                .getEnrollListByBoardIdAndStatus(eq(boardId), eq(AcceptStatus.PENDING), any(Pageable.class));
    }

    // ── getEnrollReceiveList ────────────────────────────────────────

    @Test
    @DisplayName("대기 중 신청이 있는 게시글이 없으면 신청 목록을 조회하지 않고 빈 결과를 반환한다")
    void 받은_신청이_없으면_신청_상세조회를_하지_않는다() {
        // given
        Long writerId = 2L;
        given(enrollReader.getBoardIdsWithPendingEnrolls(eq(writerId), any(Pageable.class)))
                .willReturn(Page.empty(PageRequest.of(0, 10)));

        // when
        PagedResponse<EnrollReceiveResponse> response = sut.getEnrollReceiveList(writerId, 0, 10);

        // then
        assertThat(response.getContent()).isEmpty();
        then(enrollReader).should(never()).getEnrollListByBoardIds(any());
        then(boardFetchPort).shouldHaveNoInteractions();
        then(userFetchPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("게시글별로 해당 게시글의 신청만 묶고, 신청이 없는 게시글은 결과에서 제외한다")
    void 게시글별로_자기_신청만_묶고_신청없는_게시글은_제외한다() {
        // given
        Long writerId = 2L, boardId = 10L, emptyBoardId = 11L;
        given(enrollReader.getBoardIdsWithPendingEnrolls(eq(writerId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(boardId, emptyBoardId), PageRequest.of(0, 10), 2));
        given(enrollReader.getEnrollListByBoardIds(List.of(boardId, emptyBoardId)))
                .willReturn(List.of(
                        enroll(100L, 1L, boardId, writerId, AcceptStatus.PENDING, true),
                        enroll(101L, 3L, boardId, writerId, AcceptStatus.PENDING, true)
                ));
        given(boardFetchPort.getBoards(List.of(boardId, emptyBoardId)))
                .willReturn(List.of(board(boardId, writerId), board(emptyBoardId, writerId)));
        given(userFetchPort.getUsers(List.of(1L, 3L))).willReturn(List.of(user(1L, null), user(3L, null)));
        given(userFetchPort.getUsers(List.of(writerId))).willReturn(List.of(user(writerId, null)));

        // when
        PagedResponse<EnrollReceiveResponse> response = sut.getEnrollReceiveList(writerId, 0, 10);

        // then
        assertThat(response.getContent()).hasSize(1);
        EnrollReceiveResponse receive = response.getContent().get(0);
        assertThat(receive.boardResponse().boardId()).isEqualTo(boardId);
        assertThat(receive.enrollResponses()).hasSize(2);
        assertThat(receive.enrollResponses()).extracting(EnrollResponse::enrollId)
                .containsExactly(100L, 101L);
        assertThat(response.getTotalElements()).isEqualTo(2);
        // 게시글 수와 무관하게 보드 조회는 1회 (N+1 회귀 방지)
        then(boardFetchPort).should().getBoards(List.of(boardId, emptyBoardId));
        then(boardFetchPort).should(never()).getBoard(any());
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

    private EnrollBoardInfo board(Long boardId, Long writerId) {
        return new EnrollBoardInfo(boardId, writerId, "제목", "내용", 1, 4, null, null);
    }

    private EnrollUserInfo user(Long userId, Long clubId) {
        return new EnrollUserInfo(userId, clubId, "홍길동", "test@catchmate.com",
                null, 'M', null, null, "USER");
    }

    private EnrollClubInfo club(Long clubId) {
        return new EnrollClubInfo(clubId, "LG 트윈스", "잠실", "서울");
    }
}
