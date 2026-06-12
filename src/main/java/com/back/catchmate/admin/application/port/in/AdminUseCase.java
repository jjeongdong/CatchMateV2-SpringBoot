package com.back.catchmate.admin.application.port.in;

import com.back.catchmate.admin.application.dto.command.InquiryAnswerCommand;
import com.back.catchmate.admin.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.admin.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.admin.application.dto.response.AdminBoardDetailWithEnrollResponse;
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
import com.back.catchmate.admin.application.dto.response.InquiryAnswerResponse;
import com.back.catchmate.admin.application.dto.response.NoticeActionResponse;
import com.back.catchmate.admin.application.dto.response.NoticeCreateResponse;
import com.back.catchmate.admin.application.dto.response.ReportActionResponse;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.notice.application.dto.response.NoticeDetailResponse;

public interface AdminUseCase {
    NoticeCreateResponse createNotice(Long userId, NoticeCreateCommand command);
    InquiryAnswerResponse createInquiryAnswer(InquiryAnswerCommand command);
    AdminDashboardResponse getDashboardStats();
    AdminUserDetailResponse getUser(Long userId);
    PagedResponse<AdminUserResponse> getUserList(String clubName, int page, int size);
    AdminBoardDetailWithEnrollResponse getBoardWithEnrollList(Long boardId);
    PagedResponse<AdminBoardResponse> getBoardListByUserId(Long userId, int page, int size);
    PagedResponse<AdminBoardResponse> getBoardList(int page, int size);
    AdminReportDetailResponse getReport(Long reportId);
    PagedResponse<AdminReportResponse> getReportList(int page, int size);
    AdminInquiryDetailResponse getInquiry(Long inquiryId);
    PagedResponse<AdminInquiryResponse> getInquiryList(int page, int size);
    AdminNoticeDetailResponse getNotice(Long noticeId);
    PagedResponse<AdminNoticeResponse> getNoticeList(int page, int size);
    ReportActionResponse updateReportProcess(Long reportId);
    NoticeDetailResponse updateNotice(Long noticeId, NoticeUpdateCommand command);
    NoticeActionResponse deleteNotice(Long noticeId);
}
