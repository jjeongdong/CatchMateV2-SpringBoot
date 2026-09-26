package com.back.catchmate.notice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.notice.application.dto.api.NoticeInfo;
import com.back.catchmate.notice.domain.NoticeRepository;
import com.back.catchmate.notice.fixture.NoticeFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeQueryApiTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeQueryApi noticeQueryApi;

    @Test
    @DisplayName("최신 공지를 limit 건까지 NoticeInfo 로 돌려준다")
    void getLatestInfos() {
        // given
        given(noticeRepository.findAllLatest(0L, 1000)).willReturn(List.of(NoticeFixture.notice(1L, 1L)));

        // when
        List<NoticeInfo> infos = noticeQueryApi.getLatestInfos(1000);

        // then
        assertThat(infos).containsExactly(new NoticeInfo(1L, "공지 1", "내용 1", NoticeFixture.CREATED_AT));
    }
}
