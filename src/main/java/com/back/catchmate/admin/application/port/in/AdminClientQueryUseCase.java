package com.back.catchmate.admin.application.port.in;

import com.back.catchmate.admin.application.dto.response.AdminBoardDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminBoardResponse;
import com.back.catchmate.admin.application.dto.response.AdminDashboardResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminInquiryResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminNoticeResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminReportResponse;
import com.back.catchmate.admin.application.dto.response.AdminUserDetailResponse;
import com.back.catchmate.admin.application.dto.response.AdminUserResponse;
import com.back.catchmate.common.response.PagedResponse;

public interface AdminClientQueryUseCase {
    AdminDashboardResponse getDashboardStats();

    AdminUserDetailResponse getUser(Long userId);

    PagedResponse<AdminUserResponse> getUserList(String clubName, int page, int size);

    AdminBoardDetailResponse getBoardWithEnrollList(Long boardId);

    PagedResponse<AdminBoardResponse> getBoardListByUserId(Long userId, int page, int size);

    PagedResponse<AdminBoardResponse> getBoardList(int page, int size);

    AdminReportDetailResponse getReport(Long reportId);

    PagedResponse<AdminReportResponse> getReportList(int page, int size);

    AdminInquiryDetailResponse getInquiry(Long inquiryId);

    PagedResponse<AdminInquiryResponse> getInquiryList(int page, int size);

    AdminNoticeDetailResponse getNotice(Long noticeId);

    PagedResponse<AdminNoticeResponse> getNoticeList(int page, int size);
}
