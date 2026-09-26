package com.back.catchmate.enroll.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.fixture.EnrollFixture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollQueryApiTest {

    @Mock
    private EnrollRepository enrollRepository;

    @InjectMocks
    private EnrollQueryApi enrollQueryApi;

    @Test
    @DisplayName("신청 상태를 이름으로 돌려주고, 입력이 비면 조회하지 않는다")
    void getAcceptStatuses() {
        // given
        given(enrollRepository.findAcceptStatusesByIds(List.of(1L))).willReturn(Map.of(1L, AcceptStatus.PENDING));
        given(enrollRepository.findAcceptStatusById(2L)).willReturn(Optional.of(AcceptStatus.ACCEPTED));

        // when & then
        assertThat(enrollQueryApi.getAcceptStatuses(List.of(1L))).containsEntry(1L, "PENDING");
        assertThat(enrollQueryApi.getAcceptStatuses(List.of())).isEmpty();
        assertThat(enrollQueryApi.findAcceptStatus(2L)).contains("ACCEPTED");
        then(enrollRepository).should().findAcceptStatusesByIds(List.of(1L));
    }

    @Test
    @DisplayName("내 신청과 게시글의 대기 신청을 정보로 바꾼다")
    void findInfoAndPendingInfos() {
        // given
        given(enrollRepository.findByApplicantIdAndBoardId(1L, 10L))
                .willReturn(Optional.of(EnrollFixture.pending(100L, 1L, 10L, 2L)));
        given(enrollRepository.findPendingByBoardIds(List.of(10L)))
                .willReturn(List.of(EnrollFixture.pending(100L, 1L, 10L, 2L)));

        // when & then
        assertThat(enrollQueryApi.findInfo(1L, 10L)).hasValueSatisfying(info -> {
            assertThat(info.enrollId()).isEqualTo(100L);
            assertThat(info.acceptStatus()).isEqualTo("PENDING");
        });
        assertThat(enrollQueryApi.getPendingInfosByBoardId(10L)).hasSize(1);
    }
}
