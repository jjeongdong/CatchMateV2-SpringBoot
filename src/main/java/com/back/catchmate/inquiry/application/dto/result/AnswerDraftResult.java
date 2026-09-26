package com.back.catchmate.inquiry.application.dto.result;

import com.back.catchmate.inquiry.domain.AnswerDraft;
import java.util.List;

/**
 * 답변 초안 생성 결과. 관리자가 검수·수정 후 답변 등록(/answer)으로 저장한다.
 *
 * @param grounded 근거 충분 여부. false 면 draft 는 직접 작성 안내 문구.
 * @param draft    생성된 초안 (또는 fallback 안내)
 * @param sources  초안 근거 출처 라벨 목록
 */
public record AnswerDraftResult(boolean grounded, String draft, List<String> sources) {
    public static AnswerDraftResult from(AnswerDraft answerDraft) {
        return new AnswerDraftResult(answerDraft.grounded(), answerDraft.draftText(), answerDraft.sources());
    }
}
