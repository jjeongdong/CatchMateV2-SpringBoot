package com.back.catchmate.inquiry.application;

import com.back.catchmate.inquiry.application.dto.result.AnswerDraftResult;
import com.back.catchmate.inquiry.application.dto.result.CorpusReindexResult;
import com.back.catchmate.inquiry.domain.AnswerAssistant;
import com.back.catchmate.inquiry.domain.CorpusDoc;
import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.notice.application.NoticeQueryApi;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// @Transactional 을 붙이지 말 것.
// 초안 생성·재색인은 OpenAI(LLM·임베딩)를 호출해 수 초가 걸린다. 트랜잭션이 이 호출을 감싸면 그동안 DB 커넥션을
// 붙잡아 인스턴스당 pool=15 가 마른다. 다만 지금은 OSIV(spring.jpa.open-in-view 기본값 true)와
// Hibernate 의 DELAYED_ACQUISITION_AND_HOLD 때문에 첫 조회에서 잡은 커넥션이 요청 끝까지 유지된다.
// 실제 효과는 OSIV 를 끌 때 생긴다.
@Service
@RequiredArgsConstructor
public class InquiryAssistService {
    // 코퍼스 색인용 일괄 조회 상한 (소규모 코퍼스 전제)
    private static final int MAX_CORPUS_FETCH = 1000;
    // 벡터 문서 id(sourceType-sourceId)와 검색 품질이 이 형식에 달려 있어 바꾸지 않는다.
    private static final String NOTICE_SOURCE = "NOTICE";
    private static final String ANSWERED_INQUIRY_SOURCE = "ANSWERED_INQUIRY";

    private final InquiryRepository inquiryRepository;
    private final AnswerAssistant answerAssistant;

    private final NoticeQueryApi noticeQueryApi;

    public AnswerDraftResult draftAnswer(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.getById(inquiryId);
        return AnswerDraftResult.from(answerAssistant.draft(inquiry.getContent()));
    }

    public CorpusReindexResult reindexCorpus() {
        List<CorpusDoc> docs = new ArrayList<>();
        noticeQueryApi
                .getLatestInfos(MAX_CORPUS_FETCH)
                .forEach(notice -> docs.add(
                        new CorpusDoc(NOTICE_SOURCE, notice.noticeId(), notice.title() + "\n" + notice.content())));
        inquiryRepository
                .findAllLatestAnswered(MAX_CORPUS_FETCH)
                .forEach(inquiry -> docs.add(new CorpusDoc(
                        ANSWERED_INQUIRY_SOURCE,
                        inquiry.getId(),
                        inquiry.getContent() + "\n답변: " + inquiry.getAnswer())));

        answerAssistant.replaceCorpus(docs);
        return new CorpusReindexResult(docs.size());
    }
}
