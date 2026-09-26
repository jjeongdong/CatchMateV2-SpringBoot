package com.back.catchmate.enroll.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.exception.EnrollNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollRepositoryImplTest {

    @Mock
    private EnrollJpaRepository enrollJpaRepository;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @InjectMocks
    private EnrollRepositoryImpl enrollRepository;

    @Test
    @DisplayName("없는 신청을 getById 하면 EnrollNotFoundException")
    void getByIdThrows() {
        given(enrollJpaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> enrollRepository.getById(1L)).isInstanceOf(EnrollNotFoundException.class);
    }

    @Test
    @DisplayName("신청 상태 조회 결과(id, 상태 쌍)를 맵으로 바꾼다")
    void findAcceptStatusesByIds() {
        given(enrollJpaRepository.findIdAndAcceptStatusByIdIn(List.of(1L, 2L)))
                .willReturn(List.of(new Object[] {1L, AcceptStatus.PENDING}, new Object[] {2L, AcceptStatus.ACCEPTED}));

        assertThat(enrollRepository.findAcceptStatusesByIds(List.of(1L, 2L)))
                .containsEntry(1L, AcceptStatus.PENDING)
                .containsEntry(2L, AcceptStatus.ACCEPTED);
    }
}
