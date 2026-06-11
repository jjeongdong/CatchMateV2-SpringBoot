package com.back.catchmate.user.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class UserRegisterCommand {
    private String signupToken;
    private Character gender;
    private String nickName;
    private LocalDate birthDate;
    private Long favoriteClubId;
    private String watchStyle;
}
