package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminUserInfo;
import com.back.catchmate.admin.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserAdminQueryUseCase;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;
    private final UserAdminQueryUseCase userAdminQueryUseCase;

    @Override
    public AdminUserInfo getUser(Long userId) {
        return fromInternalResponse(userInternalQueryUseCase.getUser(userId));
    }

    @Override
    public List<AdminUserInfo> getUsers(List<Long> userIds) {
        return userInternalQueryUseCase.getUsers(userIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    @Override
    public Page<AdminUserInfo> getUsersByClubId(Long clubId, Pageable pageable) {
        return userAdminQueryUseCase.getUsersByClubId(clubId, pageable).map(this::fromInternalResponse);
    }

    @Override
    public Map<Long, Long> getUserCountByClubId() {
        return userAdminQueryUseCase.getUserCountByClubId();
    }

    @Override
    public Map<String, Long> getUserCountByWatchStyle() {
        return userAdminQueryUseCase.getUserCountByWatchStyle();
    }

    @Override
    public long getTotalUserCount() {
        return userAdminQueryUseCase.getTotalUserCount();
    }

    @Override
    public long getUserCountByGender(Character gender) {
        return userAdminQueryUseCase.getUserCountByGender(gender);
    }

    private AdminUserInfo fromInternalResponse(UserInternalResponse response) {
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
