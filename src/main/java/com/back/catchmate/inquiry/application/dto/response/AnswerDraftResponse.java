package com.back.catchmate.inquiry.application.dto.response;

import java.util.List;

/**
 * 답변 초안 생성 API 응답. 운영자가 검수·수정 후 기존 답변 등록 흐름으로 저장한다.
 *
 * @param grounded 근거가 충분한지 여부. false 면 draft 는 "직접 작성" 안내 문구.
 * @param draft    생성된 초안 (또는 fallback 안내)
 * @param sources  초안 근거 출처 라벨 목록
 */
public record AnswerDraftResponse(
        boolean grounded,
        String draft,
        List<String> sources
) {
}
