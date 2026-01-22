package com.back.catchmate.api.user.dto;

import com.back.catchmate.application.user.dto.UserRegisterCommand;
import com.back.catchmate.domain.user.model.Provider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {

    @Email(message = "email 형식이 올바르지 않습니다.")
    @NotBlank(message = "email은 필수 값입니다.")
    private String email;

    @NotBlank(message = "providerId는 필수 값입니다.")
    private String providerId;

    @NotNull(message = "provider는 필수 값입니다.")
    private Provider provider;

    @NotBlank(message = "profileImageUrl은 필수 값입니다.")
    private String profileImageUrl;

    @NotBlank(message = "fcmToken은 필수 값입니다.")
    private String fcmToken;

    @NotNull(message = "gender는 필수 값입니다.")
    private Character gender;

    @NotBlank(message = "nickName은 필수 값입니다.")
    @Size(min = 2, max = 10, message = "nickName은 2자 이상 10자 이하여야 합니다.")
    private String nickName;

    @NotNull(message = "birthDate는 필수 값입니다.")
    private LocalDate birthDate;

    @NotNull(message = "favoriteClubId는 필수 값입니다.")
    private Long favoriteClubId;

    // 선택 값
    private String watchStyle;

    private static final String SEPARATOR = "@";

    public UserRegisterCommand toCommand() {
        return UserRegisterCommand.builder()
                .provider(provider)
                .providerIdWithProvider(providerId + SEPARATOR + provider)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .fcmToken(fcmToken)
                .gender(gender)
                .nickName(nickName)
                .birthDate(birthDate)
                .favoriteClubId(favoriteClubId)
                .watchStyle(watchStyle)
                .build();
    }
}
