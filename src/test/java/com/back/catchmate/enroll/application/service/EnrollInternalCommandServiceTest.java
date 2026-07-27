package com.back.catchmate.enroll.application.service;

import com.back.catchmate.enroll.application.port.out.persistence.EnrollRepository;
import com.back.catchmate.enroll.domain.model.Enroll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class EnrollInternalCommandServiceTest {

    @Mock
    private EnrollRepository enrollRepository;

    @Mock
    private EnrollReader enrollReader;

    @InjectMocks
    private EnrollInternalCommandService sut;

    @Test
    @DisplayName("두 사용자 사이의 수락된 신청을 모두 삭제한다")
    void 두_사용자_사이의_수락된_신청을_모두_삭제한다() {
        // given
        Long blockerId = 1L;
        Long blockedId = 2L;
        Enroll enrollA = Enroll.createEnroll(blockedId, 10L, blockerId, "직관 같이가요");
        Enroll enrollB = Enroll.createEnroll(blockedId, 11L, blockerId, "하나 더");
        given(enrollReader.getAcceptedEnrollsBetween(blockerId, blockedId))
                .willReturn(List.of(enrollA, enrollB));

        // when
        sut.deleteAcceptedEnrollsBetween(blockerId, blockedId);

        // then
        then(enrollRepository).should().delete(enrollA);
        then(enrollRepository).should().delete(enrollB);
    }

    @Test
    @DisplayName("삭제 대상 수락 신청이 없으면 삭제를 호출하지 않는다")
    void 삭제_대상이_없으면_삭제를_호출하지_않는다() {
        // given
        Long blockerId = 1L;
        Long blockedId = 2L;
        given(enrollReader.getAcceptedEnrollsBetween(blockerId, blockedId))
                .willReturn(List.of());

        // when
        sut.deleteAcceptedEnrollsBetween(blockerId, blockedId);

        // then
        then(enrollRepository).should(never()).delete(any());
    }
}
