package com.back.catchmate.inquiry.application.port.in;

/**
 * 코퍼스 색인 정문 — 관리자가 수동으로 호출해 공지+답변완료 문의를 벡터 스토어에 재적재한다.
 *
 * @return 색인된 문서 수
 */
public interface InquiryAssistIndexUseCase {
    int reindex();
}
