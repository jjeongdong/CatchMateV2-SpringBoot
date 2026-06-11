package com.back.catchmate.oauth.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class SignUpCommand {
    private String signupToken;
    private Character gender;
    private String nickName;
    private LocalDate birthDate;
    private Long favoriteClubId;
    private String watchStyle;
}
