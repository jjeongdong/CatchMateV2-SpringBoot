package com.back.catchmate.notice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import com.back.catchmate.notice.application.dto.result.NoticeResult;
import com.back.catchmate.notice.domain.NoticeRepository;
import com.back.catchmate.notice.fixture.NoticeFixture;
import com.back.catchmate.user.application.UserQueryApi;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeQueryServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private UserQueryApi userQueryApi;

    @InjectMocks
    private NoticeQueryService noticeQueryService;

    @Test
    @DisplayName("목록은 작성자 닉네임을 한 번에 조회해 붙이고, 조회되지 않는 작성자는 빈 문자열이다")
    void listCombinesNicknames() {
        // given
        given(noticeRepository.findAllLatest(0L, 2))
                .willReturn(List.of(NoticeFixture.notice(2L, 1L), NoticeFixture.notice(1L, 9L)));
        given(noticeRepository.count()).willReturn(3L);
        given(userQueryApi.getInfos(List.of(1L, 9L)))
                .willReturn(Map.of(1L, NoticeCommandServiceTest.writer(1L, "관리자")));

        // when
        OffsetPageResult<NoticeResult> result = noticeQueryService.getNotices(0, 2);

        // then
        assertThat(result.content()).extracting(NoticeResult::writerNickname).containsExactly("관리자", "");
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("공지가 없으면 유저를 조회하지 않는다")
    void skipsUserLookupWhenEmpty() {
        // given
        given(noticeRepository.findAllLatest(0L, 20)).willReturn(List.of());
        given(noticeRepository.count()).willReturn(0L);

        // when
        OffsetPageResult<NoticeResult> result = noticeQueryService.getNotices(0, 20);

        // then
        assertThat(result.content()).isEmpty();
        then(userQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상세는 작성자 닉네임을 담는다")
    void detail() {
        // given
        given(noticeRepository.getById(1L)).willReturn(NoticeFixture.notice(1L, 1L));
        given(userQueryApi.getInfo(1L)).willReturn(NoticeCommandServiceTest.writer(1L, "관리자"));

        // when
        NoticeDetailResult result = noticeQueryService.getNotice(1L);

        // then
        assertThat(result).isEqualTo(new NoticeDetailResult(1L, "공지 1", "내용 1", "관리자", NoticeFixture.CREATED_AT));
    }
}
