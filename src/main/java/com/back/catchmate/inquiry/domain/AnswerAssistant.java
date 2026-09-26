package com.back.catchmate.inquiry.domain;

import java.util.List;

// 답변 초안 생성(검색 + LLM)과 코퍼스 적재를 한 포트로 둔다 — 둘이 같은 벡터 스토어를 공유한다.
public interface AnswerAssistant {

    AnswerDraft draft(String question);

    // 이전에 적재한 문서를 모두 지우고 docs 로 다시 채운다.
    void replaceCorpus(List<CorpusDoc> docs);
}
