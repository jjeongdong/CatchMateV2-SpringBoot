package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminAnswerDraftInfo;

/**
 * cross-context 출력 포트 — 문의 답변 초안 생성을 inquiry 컨텍스트에 위임한다.
 * 구현({@code AdminAnswerAssistFetchAdapter})이 inquiry 정문(InquiryAnswerAssistUseCase)을 호출한다.
 */
public interface AnswerAssistFetchPort {
    AdminAnswerDraftInfo getAnswerDraft(Long inquiryId);
}
