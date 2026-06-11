package com.back.catchmate.api.user.dto.request;

import com.back.catchmate.orchestration.user.dto.command.UserRegisterCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {
    @NotBlank(message = "signupToken은 필수 값입니다.")
    private String signupToken;

    @NotNull(message = "gender는 필수 값입니다.")
    private Character gender;

    @NotBlank(message = "nickName은 필수 값입니다.")
    @Size(min = 2, max = 10, message = "nickName은 2자 이상 10자 이하여야 합니다.")
    private String nickName;

    @NotNull(message = "birthDate는 필수 값입니다.")
    private LocalDate birthDate;

    @NotNull(message = "응원 구단은 필수 값입니다.")
    private Long favoriteClubId;

    private String watchStyle;

    public UserRegisterCommand toCommand() {
        return UserRegisterCommand.builder()
                .signupToken(signupToken)
                .gender(gender)
                .nickName(nickName)
                .birthDate(birthDate)
                .favoriteClubId(favoriteClubId)
                .watchStyle(watchStyle)
                .build();
    }
}
