package com.back.catchmate.oauth.adapter.in.web.dto.request;

import com.back.catchmate.oauth.application.dto.command.SignUpCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SignUpRequest(
        @NotBlank(message = "signupToken은 필수 값입니다.") String signupToken,
        @NotNull(message = "gender는 필수 값입니다.") Character gender,
        @NotBlank(message = "nickName은 필수 값입니다.") @Size(min = 2, max = 10, message = "nickName은 2자 이상 10자 이하여야 합니다.") String nickName,
        @NotNull(message = "birthDate는 필수 값입니다.") LocalDate birthDate,
        @NotNull(message = "응원 구단은 필수 값입니다.") Long favoriteClubId,
        String watchStyle
) {
    public SignUpCommand toCommand() {
        return new SignUpCommand(
                signupToken,
                gender,
                nickName,
                birthDate,
                favoriteClubId,
                watchStyle
        );
    }
}
