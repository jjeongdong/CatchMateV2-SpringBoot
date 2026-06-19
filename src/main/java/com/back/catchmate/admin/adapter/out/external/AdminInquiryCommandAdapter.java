package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.dto.command.InquiryRegisterAnswerCommand;
import com.back.catchmate.admin.application.port.out.external.InquiryCommandPort;
import com.back.catchmate.inquiry.application.dto.command.InquiryInternalRegisterAnswerCommand;
import com.back.catchmate.inquiry.application.port.in.InquiryInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInquiryCommandAdapter implements InquiryCommandPort {
    private final InquiryInternalCommandUseCase inquiryInternalCommandUseCase;

    @Override
    public void registerAnswer(InquiryRegisterAnswerCommand command) {
        inquiryInternalCommandUseCase.registerAnswer(
                new InquiryInternalRegisterAnswerCommand(command.inquiryId(), command.content())
        );
    }
}
