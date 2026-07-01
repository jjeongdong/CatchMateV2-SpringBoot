package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.inquiry.application.port.in.InquiryAssistIndexUseCase;
import com.back.catchmate.inquiry.application.port.out.dto.CorpusDoc;
import com.back.catchmate.inquiry.application.port.out.external.AssistCorpusPort;
import com.back.catchmate.inquiry.application.port.out.external.NoticeFetchPort;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 코퍼스 색인 조율 — 공지(cross-context)와 답변완료 문의(own)를 {@link CorpusDoc} 로 모아
 * 벡터 스토어를 전량 재구축한다. 외부 RAG 호출은 {@link AssistCorpusPort} 뒤에 격리된다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryAssistIndexService implements InquiryAssistIndexUseCase {
    // 답변완료 문의 일괄 조회 상한 (소규모 코퍼스 전제)
    private static final int MAX_CORPUS_FETCH = 1000;

    private final InquiryReader inquiryReader;
    private final NoticeFetchPort noticeFetchPort;
    private final AssistCorpusPort assistCorpusPort;

    @Override
    public int reindex() {
        List<CorpusDoc> docs = new ArrayList<>();

        noticeFetchPort.fetchAll().forEach(notice ->
                docs.add(new CorpusDoc("NOTICE", notice.noticeId(),
                        notice.title() + "\n" + notice.content())));

        inquiryReader.getInquiryList(PageRequest.of(0, MAX_CORPUS_FETCH)).getContent().stream()
                .filter(inquiry -> inquiry.getStatus() == InquiryStatus.ANSWERED && inquiry.getAnswer() != null)
                .forEach(inquiry -> docs.add(new CorpusDoc("ANSWERED_INQUIRY", inquiry.getId(),
                        inquiry.getContent() + "\n답변: " + inquiry.getAnswer())));

        assistCorpusPort.clear();
        assistCorpusPort.upsert(docs);
        return docs.size();
    }
}
