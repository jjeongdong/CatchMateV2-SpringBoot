package com.back.catchmate.inquiry.domain;

/**
 * 벡터 스토어에 색인되는 한 건의 표준 단위. 공지든 답변완료 문의든 이 형태로 {@link AnswerAssistant} 에 넘긴다.
 *
 * @param sourceType 출처 종류 (예: "NOTICE", "ANSWERED_INQUIRY")
 * @param sourceId   원본 식별자 (검색 결과에서 출처 추적용)
 * @param text       임베딩 대상 본문
 */
public record CorpusDoc(String sourceType, Long sourceId, String text) {}
