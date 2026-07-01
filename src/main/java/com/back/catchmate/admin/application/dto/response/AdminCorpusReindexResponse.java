package com.back.catchmate.admin.application.dto.response;

/**
 * 코퍼스 수동 재색인 결과.
 *
 * @param indexedCount 벡터 스토어에 적재된 문서 수 (공지 + 답변완료 문의)
 */
public record AdminCorpusReindexResponse(
        int indexedCount
) {
    public static AdminCorpusReindexResponse of(int indexedCount) {
        return new AdminCorpusReindexResponse(indexedCount);
    }
}
