package com.back.catchmate.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.report.application.dto.result.ReportDetailResult;
import com.back.catchmate.report.application.dto.result.ReportResult;
import com.back.catchmate.report.domain.ReportRepository;
import com.back.catchmate.report.fixture.ReportFixture;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserQueryApi userQueryApi;

    @InjectMocks
    private ReportQueryService reportQueryService;

    static UserInfo user(Long userId, String nickName) {
        return new UserInfo(
                userId,
                nickName + "@catchmate.com",
                null,
                null,
                'M',
                nickName,
                null,
                null,
                "img-" + userId,
                "ROLE_USER",
                null,
                1L,
                false,
                false,
                false,
                false,
                null,
                null);
    }

    @Test
    @DisplayName("상세는 신고자·피신고자 정보를 담는다")
    void detail() {
        // given
        given(reportRepository.getById(5L)).willReturn(ReportFixture.report(5L, 1L, 2L));
        given(userQueryApi.getInfo(1L)).willReturn(user(1L, "신고자"));
        given(userQueryApi.getInfo(2L)).willReturn(user(2L, "피신고자"));

        // when
        ReportDetailResult result = reportQueryService.getReport(5L);

        // then
        assertThat(result.reporterNickname()).isEqualTo("신고자");
        assertThat(result.reportedUserNickname()).isEqualTo("피신고자");
        assertThat(result.reportedUserProfileImage()).isEqualTo("img-2");
        assertThat(result.reason()).isEqualTo("SPAM");
    }

    @Test
    @DisplayName("목록에서 신고자가 탈퇴해 조회되지 않으면 신고자 닉네임만 null 이다")
    void leavesReporterNullWhenMissing() {
        // given
        given(reportRepository.findAllLatest(0L, 20))
                .willReturn(List.of(ReportFixture.report(6L, 1L, 2L), ReportFixture.report(5L, 3L, 2L)));
        given(reportRepository.count()).willReturn(2L);
        given(userQueryApi.getInfos(List.of(1L, 3L))).willReturn(Map.of(1L, user(1L, "신고자")));

        // when
        OffsetPageResult<ReportResult> result = reportQueryService.getReports(0, 20);

        // then
        assertThat(result.content()).extracting(ReportResult::reporterNickname).containsExactly("신고자", null);
        assertThat(result.content()).extracting(ReportResult::reporterId).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("신고가 없으면 유저를 조회하지 않는다")
    void skipsUserLookupWhenEmpty() {
        // given
        given(reportRepository.findAllLatest(0L, 20)).willReturn(List.of());
        given(reportRepository.count()).willReturn(0L);

        // when
        reportQueryService.getReports(0, 20);

        // then
        then(userQueryApi).shouldHaveNoInteractions();
    }
}
