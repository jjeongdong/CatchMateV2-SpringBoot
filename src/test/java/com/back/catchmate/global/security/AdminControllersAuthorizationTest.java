package com.back.catchmate.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.admin.presentation.AdminDashboardController;
import com.back.catchmate.board.presentation.AdminBoardController;
import com.back.catchmate.inquiry.presentation.AdminInquiryController;
import com.back.catchmate.notice.presentation.AdminNoticeController;
import com.back.catchmate.report.presentation.AdminReportController;
import com.back.catchmate.user.presentation.AdminUserController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.prepost.PreAuthorize;

// SecurityConfig 에 /api/admin/** 규칙이 없어 관리자 보호는 컨트롤러의 @PreAuthorize 뿐이다.
// 누락되면 일반 사용자가 관리자 API 를 호출할 수 있으므로 어노테이션 존재를 못 박는다.
class AdminControllersAuthorizationTest {

    @ParameterizedTest
    @ValueSource(
            classes = {
                AdminBoardController.class,
                AdminDashboardController.class,
                AdminInquiryController.class,
                AdminNoticeController.class,
                AdminReportController.class,
                AdminUserController.class
            })
    @DisplayName("관리자 컨트롤러는 ADMIN 권한만 허용한다")
    void requiresAdminRole(Class<?> controller) {
        PreAuthorize preAuthorize = controller.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
    }
}
