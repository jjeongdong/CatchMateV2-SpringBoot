package com.back.catchmate.domain.user.service;

import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.repository.UserRepository;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User registerUser(User user) {
        Optional<User> existingUser = userRepository.findByProviderId(user.getProviderId());
        if (existingUser.isPresent()) {
            throw new BaseException(ErrorCode.USER_ALREADY_EXISTS);
        }

        return userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    public Optional<User> findByProviderId(String providerIdWithProvider) {
        return userRepository.findByProviderId(providerIdWithProvider);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    public boolean checkNickname(String nickName) {
        return userRepository.existsByNickName(nickName);
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getUserCountByGender(Character gender) {
        return userRepository.countByGender(gender);
    }

    public Map<String, Long> getUserCountByClub() {
        return userRepository.countUsersByClub();
    }

    public Map<String, Long> getUserCountByAgeGroup() {
        return userRepository.countUsersByWatchStyle();
    }

    public Map<String, Long> getUserCountByWatchStyle() {
        return userRepository.countUsersByWatchStyle();
    }

    public DomainPage<User> getUsersByClub(String clubName, DomainPageable pageable) {
        return userRepository.findAllByClubName(clubName, pageable);
    }
}
