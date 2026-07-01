package com.back.catchmate.inquiry.application.port.in;

import com.back.catchmate.inquiry.application.dto.response.AnswerDraftResponse;

/**
 * 답변 초안 생성 정문 — 운영자(컨트롤러)가 특정 문의에 대한 RAG 초안을 요청하는 진입점.
 */
public interface InquiryAnswerAssistUseCase {
    AnswerDraftResponse draftAnswer(Long inquiryId);
}
