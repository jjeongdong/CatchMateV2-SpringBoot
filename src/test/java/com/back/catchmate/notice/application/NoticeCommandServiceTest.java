package com.back.catchmate.notice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.notice.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.notice.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.notice.application.dto.result.NoticeCreateResult;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import com.back.catchmate.notice.domain.Notice;
import com.back.catchmate.notice.domain.NoticeRepository;
import com.back.catchmate.notice.domain.event.NoticeCreatedEvent;
import com.back.catchmate.notice.fixture.NoticeFixture;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NoticeCommandServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoticeCommandService noticeCommandService;

    @Test
    @DisplayName("공지를 만들면 저장하고 NoticeCreatedEvent 를 발행한다")
    void publishesNoticeCreatedEvent() {
        // given
        Notice saved = NoticeFixture.notice(10L, 1L);
        given(noticeRepository.save(any(Notice.class))).willReturn(saved);

        // when
        NoticeCreateResult result = noticeCommandService.createNotice(1L, new NoticeCreateCommand("공지 10", "내용"));

        // then
        assertThat(result).isEqualTo(new NoticeCreateResult(10L, NoticeFixture.CREATED_AT));
        then(eventPublisher).should().publishEvent(new NoticeCreatedEvent(10L, "공지 10"));
    }

    @Test
    @DisplayName("공지를 고치면 작성자 닉네임을 담은 상세를 돌려준다")
    void updateReturnsDetail() {
        // given
        given(noticeRepository.getById(10L)).willReturn(NoticeFixture.notice(10L, 1L));
        given(userQueryApi.getInfo(1L)).willReturn(writer(1L, "관리자"));

        // when
        NoticeDetailResult result = noticeCommandService.updateNotice(10L, new NoticeUpdateCommand("새 제목", "새 내용"));

        // then
        assertThat(result.title()).isEqualTo("새 제목");
        assertThat(result.content()).isEqualTo("새 내용");
        assertThat(result.writerNickname()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("공지를 지운다")
    void deleteNotice() {
        // given
        Notice notice = NoticeFixture.notice(10L, 1L);
        given(noticeRepository.getById(10L)).willReturn(notice);

        // when
        noticeCommandService.deleteNotice(10L);

        // then
        then(noticeRepository).should().delete(notice);
    }

    static UserInfo writer(Long userId, String nickName) {
        return new UserInfo(
                userId,
                null,
                null,
                null,
                'M',
                nickName,
                null,
                null,
                null,
                "ROLE_ADMIN",
                null,
                1L,
                false,
                false,
                false,
                false,
                null,
                null);
    }
}
