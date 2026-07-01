package com.back.catchmate.notice.application.port.in;

import com.back.catchmate.notice.application.dto.response.NoticeInternalResponse;

import java.util.List;

/**
 * notice 의 non-admin 내부 읽기 정문. 다른 컨텍스트(예: inquiry 코퍼스 색인)가
 * Fetch Port 체인으로 공지를 읽을 때 진입한다. (admin 전용 페이징 조회는 {@code NoticeAdminQueryUseCase})
 */
public interface NoticeInternalQueryUseCase {
    List<NoticeInternalResponse> getAllNotices();
}
