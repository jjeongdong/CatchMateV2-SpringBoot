package com.back.catchmate.oauth.application.port.in;

import com.back.catchmate.oauth.application.dto.command.OAuthCallbackCommand;
import com.back.catchmate.oauth.application.dto.command.SignUpCommand;
import com.back.catchmate.oauth.application.dto.response.OAuthCallbackResult;
import com.back.catchmate.oauth.application.dto.response.SignUpResult;

public interface OAuthClientCommandUseCase {
    OAuthCallbackResult handleCallback(OAuthCallbackCommand command);

    SignUpResult signUp(SignUpCommand command);
}
