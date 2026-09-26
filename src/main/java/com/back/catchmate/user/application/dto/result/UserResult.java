package com.back.catchmate.user.application.dto.result;

import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.user.domain.User;
import java.time.LocalDate;

public record UserResult(
        Long userId,
        String nickName,
        String email,
        String profileImageUrl,
        Character gender,
        LocalDate birthDate,
        String watchStyle,
        ClubView club,
        String authority) {

    public static UserResult of(User user, ClubInfo club) {
        return new UserResult(
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getBirthDate(),
                user.getWatchStyle(),
                club != null ? ClubView.from(club) : null,
                user.getAuthority().name());
    }

    // 타 BC 의 ClubInfo 를 응답에 그대로 노출하지 않도록 화면용 형태로 옮긴다.
    public record ClubView(Long clubId, String name, String homeStadium, String region) {
        static ClubView from(ClubInfo club) {
            return new ClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
        }
    }
}
