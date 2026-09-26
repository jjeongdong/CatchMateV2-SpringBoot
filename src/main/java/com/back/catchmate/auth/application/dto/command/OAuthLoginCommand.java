package com.back.catchmate.auth.application.dto.command;

import com.back.catchmate.auth.domain.Provider;

// error·errorDescription 은 사용자가 동의를 거부하는 등 공급자가 실패를 돌려줄 때만 채워진다.
public record OAuthLoginCommand(
        Provider provider, String code, String state, String stateFromCookie, String error, String errorDescription) {}
