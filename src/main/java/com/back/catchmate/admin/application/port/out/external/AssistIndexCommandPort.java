package com.back.catchmate.admin.application.port.out.external;

/**
 * cross-context 출력 포트 — 코퍼스 수동 재색인을 inquiry 컨텍스트에 위임한다.
 * 구현({@code AdminAssistIndexCommandAdapter})이 inquiry 정문(InquiryAssistIndexUseCase)을 호출한다.
 *
 * @return 색인된 문서 수
 */
public interface AssistIndexCommandPort {
    int reindex();
}
