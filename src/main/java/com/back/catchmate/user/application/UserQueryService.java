package com.back.catchmate.user.application;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.user.application.dto.result.NicknameAvailabilityResult;
import com.back.catchmate.user.application.dto.result.UserAlarmResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {
    private final UserRepository userRepository;
    private final ClubQueryApi clubQueryApi;

    @Transactional(readOnly = true)
    public UserResult getMyProfile(Long userId) {
        return getUser(userId);
    }

    @Transactional(readOnly = true)
    public UserResult getUser(Long userId) {
        User user = userRepository.getById(userId);
        return UserResult.of(user, clubQueryApi.getInfo(user.getClubId()));
    }

    @Transactional(readOnly = true)
    public NicknameAvailabilityResult getNicknameAvailability(String nickName) {
        return new NicknameAvailabilityResult(nickName, !userRepository.existsByNickName(nickName));
    }

    @Transactional(readOnly = true)
    public UserAlarmResult getMyAlarms(Long userId) {
        return UserAlarmResult.from(userRepository.getById(userId));
    }
}
