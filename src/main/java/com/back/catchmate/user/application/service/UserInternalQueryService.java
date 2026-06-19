package com.back.catchmate.user.application.service;

import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserAdminQueryUseCase;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserInternalQueryService implements UserInternalQueryUseCase, UserAdminQueryUseCase {
    private final UserReader userReader;

    @Override
    public UserInternalResponse getUser(Long userId) {
        return UserInternalResponse.from(userReader.getUser(userId));
    }

    @Override
    public List<UserInternalResponse> getUsers(List<Long> userIds) {
        return userReader.getUsers(userIds).stream()
                .map(UserInternalResponse::from)
                .toList();
    }

    @Override
    public Optional<UserInternalResponse> findByProviderId(String providerIdWithProvider) {
        return userReader.findByProviderId(providerIdWithProvider)
                .map(UserInternalResponse::from);
    }

    @Override
    public List<UserInternalResponse> getEventAlarmEnabledUsers() {
        return userReader.getEventAlarmEnabledUsers().stream()
                .map(UserInternalResponse::from)
                .toList();
    }

    @Override
    public Page<UserInternalResponse> getUsersByClubId(Long clubId, Pageable pageable) {
        return userReader.getUsersByClubId(clubId, pageable).map(UserInternalResponse::from);
    }

    @Override
    public Map<Long, Long> getUserCountByClubId() {
        return userReader.getUserCountByClubId();
    }

    @Override
    public Map<String, Long> getUserCountByWatchStyle() {
        return userReader.getUserCountByWatchStyle();
    }

    @Override
    public long getTotalUserCount() {
        return userReader.getTotalUserCount();
    }

    @Override
    public long getUserCountByGender(Character gender) {
        return userReader.getUserCountByGender(gender);
    }
}
