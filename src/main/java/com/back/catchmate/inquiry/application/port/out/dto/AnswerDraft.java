package com.back.catchmate.inquiry.application.port.out.dto;

import java.util.List;

/**
 * {@code AnswerAssistPort.draftAnswer} 의 반환값 — RAG 초안 생성 결과.
 *
 * @param grounded   근거 문서가 임계값 이상 검색되어 초안을 신뢰할 수 있는지 여부.
 *                   {@code false} 면 fallback (운영자 직접 작성 안내).
 * @param draftText  생성된 답변 초안 (fallback 시 안내 문구)
 * @param sources    초안 근거로 사용된 출처 라벨 목록
 */
public record AnswerDraft(
        boolean grounded,
        String draftText,
        List<String> sources
) {
}
