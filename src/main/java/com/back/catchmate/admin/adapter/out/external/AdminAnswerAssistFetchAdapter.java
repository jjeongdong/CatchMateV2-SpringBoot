package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminAnswerDraftInfo;
import com.back.catchmate.admin.application.port.out.external.AnswerAssistFetchPort;
import com.back.catchmate.inquiry.dto.response.AnswerDraftResponse;
import com.back.catchmate.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AnswerAssistFetchPort 구현 — inquiry 정문({@link InquiryAnswerAssistUseCase})을 호출하고,
 * inquiry 타입을 admin 소유 DTO({@link AdminAnswerDraftInfo})로 격리한다.
 */
@Component
@RequiredArgsConstructor
public class AdminAnswerAssistFetchAdapter implements AnswerAssistFetchPort {
    private final InquiryService inquiryService;

    @Override
    public AdminAnswerDraftInfo getAnswerDraft(Long inquiryId) {
        AnswerDraftResponse draft = inquiryService.draftAnswer(inquiryId);
        return new AdminAnswerDraftInfo(draft.grounded(), draft.draft(), draft.sources());
    }
}
