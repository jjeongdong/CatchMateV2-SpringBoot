package com.back.catchmate.inquiry.application.port.in;

import com.back.catchmate.inquiry.application.dto.command.InquiryInternalRegisterAnswerCommand;

public interface InquiryInternalCommandUseCase {
    void registerAnswer(InquiryInternalRegisterAnswerCommand command);
}
