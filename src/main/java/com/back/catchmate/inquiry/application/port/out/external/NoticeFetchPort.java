package com.back.catchmate.inquiry.application.port.out.external;

import com.back.catchmate.inquiry.application.port.out.dto.AssistNoticeInfo;

import java.util.List;

/**
 * cross-context 출력 포트 — 코퍼스 색인에 필요한 공지를 notice 컨텍스트에서 가져온다.
 * 구현({@code InquiryNoticeFetchAdapter})이 notice 의 읽기 정문(NoticeInternalQueryUseCase)을 호출한다.
 */
public interface NoticeFetchPort {
    List<AssistNoticeInfo> fetchAll();
}
