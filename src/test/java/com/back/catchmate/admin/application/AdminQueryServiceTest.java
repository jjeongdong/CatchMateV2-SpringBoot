package com.back.catchmate.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.admin.application.dto.result.AdminDashboardResult;
import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.inquiry.application.InquiryQueryApi;
import com.back.catchmate.report.application.ReportQueryApi;
import com.back.catchmate.user.application.UserQueryApi;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
// 공통 스텁을 @BeforeEach 에 두고 테스트마다 일부만 확인하므로 느슨하게 둔다.
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminQueryServiceTest {

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private BoardQueryApi boardQueryApi;

    @Mock
    private ReportQueryApi reportQueryApi;

    @Mock
    private InquiryQueryApi inquiryQueryApi;

    @Mock
    private ClubQueryApi clubQueryApi;

    @InjectMocks
    private AdminQueryService adminQueryService;

    @BeforeEach
    void setUp() {
        given(userQueryApi.count()).willReturn(10L);
        given(userQueryApi.countByGender('M')).willReturn(6L);
        given(userQueryApi.countByGender('F')).willReturn(4L);
        given(userQueryApi.countByWatchStyles()).willReturn(Map.of("응원형", 7L));
        given(boardQueryApi.count()).willReturn(20L);
        given(reportQueryApi.count()).willReturn(3L);
        given(reportQueryApi.countPending()).willReturn(1L);
        given(inquiryQueryApi.count()).willReturn(5L);
        given(inquiryQueryApi.countWaiting()).willReturn(2L);
    }

    @Test
    @DisplayName("대시보드는 각 BC 의 수를 모으고 구단 ID 를 이름으로 바꾼다")
    void getDashboard() {
        // given
        given(userQueryApi.countByClubIds()).willReturn(Map.of(3L, 8L));
        given(clubQueryApi.getInfos(List.of(3L))).willReturn(Map.of(3L, new ClubInfo(3L, "LG 트윈스", "잠실", "서울")));

        // when
        AdminDashboardResult result = adminQueryService.getDashboard();

        // then
        assertThat(result.totalUserCount()).isEqualTo(10L);
        assertThat(result.genderRatio()).isEqualTo(new AdminDashboardResult.GenderRatio(6L, 4L));
        assertThat(result.totalBoardCount()).isEqualTo(20L);
        assertThat(result.userCountByClub()).containsExactlyEntriesOf(Map.of("LG 트윈스", 8L));
        assertThat(result.userCountByWatchStyle()).containsEntry("응원형", 7L);
        assertThat(result.pendingReportCount()).isEqualTo(1L);
        assertThat(result.waitingInquiryCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("구단 정보가 없는 구단 ID 의 집계는 빠지고 나머지는 남는다")
    void dropsUnknownClubs() {
        // given
        given(userQueryApi.countByClubIds()).willReturn(Map.of(3L, 8L, 99L, 1L));
        given(clubQueryApi.getInfos(anyCollection())).willReturn(Map.of(3L, new ClubInfo(3L, "LG 트윈스", "잠실", "서울")));

        // when
        AdminDashboardResult result = adminQueryService.getDashboard();

        // then
        assertThat(result.userCountByClub()).containsExactlyEntriesOf(Map.of("LG 트윈스", 8L));
    }

    @Test
    @DisplayName("구단별 집계가 비면 구단을 조회하지 않는다")
    void skipsClubLookupWhenEmpty() {
        // given
        given(userQueryApi.countByClubIds()).willReturn(Map.of());

        // when
        AdminDashboardResult result = adminQueryService.getDashboard();

        // then
        assertThat(result.userCountByClub()).isEmpty();
        then(clubQueryApi).shouldHaveNoInteractions();
    }
}
