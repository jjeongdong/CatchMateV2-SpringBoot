package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.inquiry.application.dto.response.AnswerDraftResponse;
import com.back.catchmate.inquiry.application.port.in.InquiryAnswerAssistUseCase;
import com.back.catchmate.inquiry.application.port.out.dto.AnswerDraft;
import com.back.catchmate.inquiry.application.port.out.external.AnswerAssistPort;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 답변 초안 생성 조율 — 문의 본문을 읽어 {@link AnswerAssistPort}(RAG)로 초안을 만든다.
 * DB 변경이 없는 읽기/생성이므로 readOnly 트랜잭션.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryAnswerAssistService implements InquiryAnswerAssistUseCase {
    private final InquiryReader inquiryReader;
    private final AnswerAssistPort answerAssistPort;

    @Override
    public AnswerDraftResponse draftAnswer(Long inquiryId) {
        Inquiry inquiry = inquiryReader.getInquiry(inquiryId);
        AnswerDraft draft = answerAssistPort.draftAnswer(inquiry.getContent());
        return new AnswerDraftResponse(draft.grounded(), draft.draftText(), draft.sources());
    }
}
