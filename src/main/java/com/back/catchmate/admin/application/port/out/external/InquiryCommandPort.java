package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.dto.command.InquiryRegisterAnswerCommand;

public interface InquiryCommandPort {
    void registerAnswer(InquiryRegisterAnswerCommand command);
}
