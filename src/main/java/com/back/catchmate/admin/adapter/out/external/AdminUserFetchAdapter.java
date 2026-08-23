package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;
import com.back.catchmate.admin.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public AdminUserInfo getUser(Long userId) {
        return fromInternalResponse(userService.getUserSummary(userId));
    }

    @Override
    public List<AdminUserInfo> getUsers(List<Long> userIds) {
        return userService.getUserSummaries(userIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    @Override
    public Page<AdminUserInfo> getUsersByClubId(Long clubId, Pageable pageable) {
        return userService.getUserSummariesByClubId(clubId, pageable).map(this::fromInternalResponse);
    }

    @Override
    public Map<Long, Long> getUserCountByClubId() {
        return userService.getUserCountByClubId();
    }

    @Override
    public Map<String, Long> getUserCountByWatchStyle() {
        return userService.getUserCountByWatchStyle();
    }

    @Override
    public long getTotalUserCount() {
        return userService.getTotalUserCount();
    }

    @Override
    public long getUserCountByGender(Character gender) {
        return userService.getUserCountByGender(gender);
    }

    private AdminUserInfo fromInternalResponse(UserSummary response) {
        return new AdminUserInfo(
                response.userId(),
                response.email(),
                response.provider(),
                response.gender(),
                response.nickName(),
                response.birthDate(),
                response.watchStyle(),
                response.profileImageUrl(),
                response.authority(),
                response.clubId(),
                response.reported(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
