package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.dto.command.InquiryRegisterAnswerCommand;
import com.back.catchmate.admin.application.port.out.external.InquiryCommandPort;
import com.back.catchmate.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInquiryCommandAdapter implements InquiryCommandPort {
    private final InquiryService inquiryService;

    @Override
    public void registerAnswer(InquiryRegisterAnswerCommand command) {
        inquiryService.registerAnswer(command.inquiryId(), command.content());
    }
}
