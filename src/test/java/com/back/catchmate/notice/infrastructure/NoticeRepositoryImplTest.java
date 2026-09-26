package com.back.catchmate.notice.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.notice.domain.exception.NoticeNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeRepositoryImplTest {

    @Mock
    private NoticeJpaRepository noticeJpaRepository;

    @InjectMocks
    private NoticeRepositoryImpl noticeRepository;

    @Test
    @DisplayName("공지가 없으면 NoticeNotFoundException 을 던진다")
    void getByIdThrowsWhenMissing() {
        // given
        given(noticeJpaRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeRepository.getById(99L)).isInstanceOf(NoticeNotFoundException.class);
    }
}
