package com.back.catchmate.user.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.user.application.port.out.persistence.UserRepository;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserReader {
    private final UserRepository userRepository;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    public List<User> getUsers(List<Long> userIds) {
        return userRepository.findAllByIds(userIds);
    }

    public Optional<User> findByProviderId(String providerIdWithProvider) {
        return userRepository.findByProviderId(providerIdWithProvider);
    }

    public List<User> getEventAlarmEnabledUsers() {
        return userRepository.findAllEventAlarmEnabled();
    }

    public Page<User> getUsersByClubId(Long clubId, Pageable pageable) {
        return userRepository.findAllByClubId(clubId, pageable);
    }

    public Map<Long, Long> getUserCountByClubId() {
        return userRepository.countUsersGroupedByClubId();
    }

    public Map<String, Long> getUserCountByWatchStyle() {
        return userRepository.countUsersByWatchStyle();
    }

    public boolean existsByNickName(String nickName) {
        return userRepository.existsByNickName(nickName);
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getUserCountByGender(Character gender) {
        return userRepository.countByGender(gender);
    }
}
