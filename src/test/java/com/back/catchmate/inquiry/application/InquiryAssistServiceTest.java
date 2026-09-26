package com.back.catchmate.inquiry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.inquiry.application.dto.result.AnswerDraftResult;
import com.back.catchmate.inquiry.application.dto.result.CorpusReindexResult;
import com.back.catchmate.inquiry.domain.AnswerAssistant;
import com.back.catchmate.inquiry.domain.AnswerDraft;
import com.back.catchmate.inquiry.domain.CorpusDoc;
import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.inquiry.fixture.InquiryFixture;
import com.back.catchmate.notice.application.NoticeQueryApi;
import com.back.catchmate.notice.application.dto.api.NoticeInfo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InquiryAssistServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private AnswerAssistant answerAssistant;

    @Mock
    private NoticeQueryApi noticeQueryApi;

    @InjectMocks
    private InquiryAssistService inquiryAssistService;

    @Test
    @DisplayName("문의 본문으로 답변 초안을 만든다")
    void draftAnswer() {
        // given
        given(inquiryRepository.getById(5L)).willReturn(InquiryFixture.waiting(5L, 1L));
        given(answerAssistant.draft("로그인이 안 돼요")).willReturn(new AnswerDraft(true, "초안", List.of("NOTICE#1")));

        // when
        AnswerDraftResult result = inquiryAssistService.draftAnswer(5L);

        // then
        assertThat(result).isEqualTo(new AnswerDraftResult(true, "초안", List.of("NOTICE#1")));
    }

    @Test
    @DisplayName("공지와 답변 완료 문의를 옛 형식 그대로 코퍼스로 만들어 교체한다")
    void buildsCorpusInLegacyFormat() {
        // given
        given(noticeQueryApi.getLatestInfos(1000))
                .willReturn(List.of(new NoticeInfo(1L, "점검 안내", "새벽 점검", InquiryFixture.CREATED_AT)));
        given(inquiryRepository.findAllLatestAnswered(1000))
                .willReturn(List.of(InquiryFixture.answered(5L, 1L, "재접속해 주세요")));

        // when
        CorpusReindexResult result = inquiryAssistService.reindexCorpus();

        // then
        then(answerAssistant)
                .should()
                .replaceCorpus(List.of(
                        new CorpusDoc("NOTICE", 1L, "점검 안내\n새벽 점검"),
                        new CorpusDoc("ANSWERED_INQUIRY", 5L, "로그인이 안 돼요\n답변: 재접속해 주세요")));
        assertThat(result).isEqualTo(new CorpusReindexResult(2));
    }
}
