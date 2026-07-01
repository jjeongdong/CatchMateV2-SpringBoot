package com.back.catchmate.inquiry.application.port.out.external;

import com.back.catchmate.inquiry.application.port.out.dto.CorpusDoc;

import java.util.List;

/**
 * RAG 인덱싱 경로 출력 포트 — 코퍼스 문서를 임베딩해 벡터 스토어에 적재한다.
 */
public interface AssistCorpusPort {
    void upsert(List<CorpusDoc> docs);

    void clear();
}
