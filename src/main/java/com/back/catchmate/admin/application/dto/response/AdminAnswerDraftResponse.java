package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminAnswerDraftInfo;

import java.util.List;

/**
 * 문의 답변 초안 생성 API 응답. 관리자가 검수·수정 후 답변 등록(/answer)으로 저장한다.
 *
 * @param grounded 근거 충분 여부. false 면 draft 는 직접 작성 안내 문구.
 * @param draft    생성된 초안 (또는 fallback 안내)
 * @param sources  초안 근거 출처 라벨 목록
 */
public record AdminAnswerDraftResponse(
        boolean grounded,
        String draft,
        List<String> sources
) {
    public static AdminAnswerDraftResponse from(AdminAnswerDraftInfo info) {
        return new AdminAnswerDraftResponse(info.grounded(), info.draft(), info.sources());
    }
}
