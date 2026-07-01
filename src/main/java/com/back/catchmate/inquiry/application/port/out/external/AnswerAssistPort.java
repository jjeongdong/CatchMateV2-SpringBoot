package com.back.catchmate.inquiry.application.port.out.external;

import com.back.catchmate.inquiry.application.port.out.dto.AnswerDraft;

/**
 * RAG 질의 경로 출력 포트 — 질문 텍스트를 받아 검색+생성으로 답변 초안을 만든다.
 * 구현({@code SpringAiAssistAdapter})만 OpenAI 를 알고, 서비스는 이 인터페이스에만 의존한다.
 */
public interface AnswerAssistPort {
    AnswerDraft draftAnswer(String question);
}
