package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.external.AssistIndexCommandPort;
import com.back.catchmate.inquiry.application.port.in.InquiryAssistIndexUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AssistIndexCommandPort 구현 — inquiry 정문({@link InquiryAssistIndexUseCase})을 호출해
 * 코퍼스를 재색인한다.
 */
@Component
@RequiredArgsConstructor
public class AdminAssistIndexCommandAdapter implements AssistIndexCommandPort {
    private final InquiryAssistIndexUseCase inquiryAssistIndexUseCase;

    @Override
    public int reindex() {
        return inquiryAssistIndexUseCase.reindex();
    }
}
