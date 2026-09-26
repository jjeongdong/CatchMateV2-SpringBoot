package com.back.catchmate.inquiry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.inquiry.application.dto.result.AdminInquiryResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryResult;
import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.inquiry.domain.exception.InquiryNotOwnerException;
import com.back.catchmate.inquiry.fixture.InquiryFixture;
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
class InquiryQueryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private UserQueryApi userQueryApi;

    @InjectMocks
    private InquiryQueryService inquiryQueryService;

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
    @DisplayName("내 문의 상세는 한국어 유형·상태와 내 닉네임을 담는다")
    void getMyInquiry() {
        // given
        given(inquiryRepository.getById(5L)).willReturn(InquiryFixture.waiting(5L, 1L));
        given(userQueryApi.getInfo(1L)).willReturn(user(1L, "작성자"));

        // when
        InquiryResult result = inquiryQueryService.getMyInquiry(1L, 5L);

        // then
        assertThat(result.nickname()).isEqualTo("작성자");
        assertThat(result.type()).isEqualTo("계정, 로그인 관련");
        assertThat(result.status()).isEqualTo("답변 대기");
    }

    @Test
    @DisplayName("남의 문의를 조회하면 InquiryNotOwnerException")
    void throwsWhenNotOwner() {
        // given
        given(inquiryRepository.getById(5L)).willReturn(InquiryFixture.waiting(5L, 1L));

        // when & then
        assertThatThrownBy(() -> inquiryQueryService.getMyInquiry(2L, 5L)).isInstanceOf(InquiryNotOwnerException.class);
        then(userQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("내 문의가 없으면 유저를 조회하지 않고 빈 페이지를 돌려준다")
    void emptyMyInquiriesSkipsUserLookup() {
        // given
        given(inquiryRepository.findAllLatestByUserId(1L, 0L, 20)).willReturn(List.of());
        given(inquiryRepository.countByUserId(1L)).willReturn(0L);

        // when
        OffsetPageResult<InquiryResult> result = inquiryQueryService.getMyInquiries(1L, 0, 20);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        then(userQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리자 목록에서 탈퇴한 작성자는 닉네임만 비우고 enum 이름으로 내린다")
    void leavesWriterNullWhenMissing() {
        // given
        given(inquiryRepository.findAllLatest(0L, 20))
                .willReturn(List.of(InquiryFixture.waiting(5L, 1L), InquiryFixture.waiting(6L, 2L)));
        given(inquiryRepository.count()).willReturn(2L);
        given(userQueryApi.getInfos(List.of(1L, 2L))).willReturn(Map.of(1L, user(1L, "남은 유저")));

        // when
        OffsetPageResult<AdminInquiryResult> result = inquiryQueryService.getInquiries(0, 20);

        // then
        assertThat(result.content())
                .extracting(AdminInquiryResult::userNickname)
                .containsExactly("남은 유저", null);
        assertThat(result.content()).extracting(AdminInquiryResult::userId).containsExactly(1L, 2L);
        assertThat(result.content().get(0).status()).isEqualTo("WAITING");
    }
}
