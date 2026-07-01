package com.back.catchmate.inquiry.adapter.out.external;

import com.back.catchmate.inquiry.application.port.out.dto.AnswerDraft;
import com.back.catchmate.inquiry.application.port.out.dto.CorpusDoc;
import com.back.catchmate.inquiry.application.port.out.external.AnswerAssistPort;
import com.back.catchmate.inquiry.application.port.out.external.AssistCorpusPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RAG 질의·색인의 실제 구현. {@link AnswerAssistPort}(검색+생성)와 {@link AssistCorpusPort}(임베딩+적재)를
 * 같은 {@link VectorStore}/{@link ChatClient} 위에서 함께 구현한다. OpenAI 의존은 이 클래스 안에만 가둔다.
 */
@Slf4j
@Component
public class SpringAiAssistAdapter implements AnswerAssistPort, AssistCorpusPort {

    private static final String META_SOURCE_TYPE = "sourceType";
    private static final String META_SOURCE_ID = "sourceId";
    private static final String FALLBACK_MESSAGE = "관련 근거 자료를 찾지 못했습니다. 직접 답변을 작성해 주세요.";

    // 고정 인사말/맺음말 — 본문은 LLM 이 쓰고, 이 틀은 코드가 감싸 매번 동일하게 유지한다. (문구는 여기서 수정)
    private static final String GREETING = "안녕하세요, catchmate 관리팀입니다.";
    private static final String CLOSING = "추가로 궁금하신 점이 있으시면 언제든 문의해 주세요.\n감사합니다.";

    // 관련성 판정은 분류(yes/no)라 결정적이어야 한다 → 동일 문의에 매번 같은 결과를 위해 temperature 0.
    private static final ChatOptions DETERMINISTIC = ChatOptions.builder().temperature(0.0).build();

    private static final String SYSTEM_PROMPT = """
            당신은 catchmate 고객지원 담당자입니다. 한국어로 답변 초안을 작성합니다.
            [참고자료]에는 공지사항(NOTICE)과 과거 답변 사례(ANSWERED_INQUIRY)가 있습니다.

            먼저 [참고자료]가 [문의]의 '구체적인 질문'에 직접 답하는 내용을 담고 있는지 엄격하게 판단하세요.
            - 기준: 참고자료 안에 문의를 해결하는 내용이 '문장으로' 실제 존재해야만 relevant=true.
              같은 주제여도(예: 둘 다 '로그인' 관련) 문의가 묻는 바로 그 문제의 해결책이 없으면 relevant=false.
            - 예시: 문의가 "비밀번호를 잊어버렸어요"인데 참고자료는 "카카오 로그인 오류 해결"만 있다면,
              둘 다 로그인 주제지만 '비밀번호 분실'에 대한 답은 없으므로 relevant=false.
            - relevant=false 면 answer 는 빈 문자열로 두고, 참고자료에 없는 해결책을 추론하거나 지어내지 마세요.
            - relevant=true 면 참고자료에 있는 내용만으로 answer 를 작성합니다.
              answer 에는 인사말("안녕하세요…")이나 맺음말("감사합니다…")을 넣지 말고 '본문'만 쓰세요.
              (인사말·맺음말은 시스템이 자동으로 감쌉니다.) 본문의 말투·문체는 과거 답변 사례
              (ANSWERED_INQUIRY의 '답변:' 부분)의 톤을 따라 기존 답변들과 이질감이 없게 합니다.
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    /** 적재한 문서 id 추적용 — clear() 시 전량 삭제하여 stale 엔트리를 제거한다. */
    private final Set<String> indexedIds = ConcurrentHashMap.newKeySet();

    public SpringAiAssistAdapter(
            ChatClient chatClient,
            VectorStore vectorStore,
            @Value("${assist.search.top-k:4}") int topK,
            @Value("${assist.search.similarity-threshold:0.6}") double similarityThreshold
    ) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public AnswerDraft draftAnswer(String question) {
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );

        // threshold 미달(=근거 없음) → LLM 호출 없이 fallback
        if (hits == null || hits.isEmpty()) {
            // 왜 하나도 안 걸렸는지 보이도록, threshold 미적용 후보 점수를 함께 찍는다.
            if (log.isInfoEnabled()) {
                List<Document> candidates = vectorStore.similaritySearch(
                        SearchRequest.builder().query(question).topK(topK).similarityThreshold(0.0).build());
                log.info("[assist] q='{}' threshold={} 통과=0건 → fallback | 후보점수: {}",
                        question, similarityThreshold, scoresOf(candidates));
            }
            return new AnswerDraft(false, FALLBACK_MESSAGE, List.of());
        }

        log.info("[assist] q='{}' threshold={} 통과={}건 | {}",
                question, similarityThreshold, hits.size(), scoresOf(hits));

        String context = hits.stream()
                .map(doc -> "- [" + sourceLabel(doc) + "] " + doc.getText())
                .collect(Collectors.joining("\n"));

        // 2단계 게이팅: 검색은 후보만 좁히고, 실제 답변 가능 여부는 LLM 이 판정한다.
        LlmDraft result = chatClient.prompt()
                .options(DETERMINISTIC)
                .system(SYSTEM_PROMPT)
                .user("[문의]\n" + question + "\n\n[참고자료]\n" + context)
                .call()
                .entity(LlmDraft.class);

        boolean relevant = result != null && result.relevant();
        log.info("[assist] q='{}' LLM relevant={}", question, relevant);

        // 유사 문서는 있었지만 실제로는 답이 안 됨 → fallback
        if (!relevant) {
            return new AnswerDraft(false, FALLBACK_MESSAGE, List.of());
        }

        List<String> sources = hits.stream()
                .map(this::sourceLabel)
                .distinct()
                .toList();

        // 고정 인사말 + LLM 본문 + 고정 맺음말
        String body = result.answer() == null ? "" : result.answer().strip();
        String draft = GREETING + "\n\n" + body + "\n\n" + CLOSING;

        return new AnswerDraft(true, draft, sources);
    }

    /** LLM 구조화 출력 — 참고자료가 문의에 실제로 답이 되는지(relevant) + 답변 초안(answer). */
    public record LlmDraft(boolean relevant, String answer) {
    }

    @Override
    public void upsert(List<CorpusDoc> docs) {
        List<Document> documents = docs.stream()
                .map(this::toDocument)
                .toList();
        documents.forEach(doc -> indexedIds.add(doc.getId()));
        vectorStore.add(documents);
    }

    @Override
    public void clear() {
        if (indexedIds.isEmpty()) {
            return;
        }
        vectorStore.delete(new ArrayList<>(indexedIds));
        indexedIds.clear();
    }

    private Document toDocument(CorpusDoc doc) {
        // 결정적 id → 재색인 시 같은 출처는 덮어쓰기(중복 방지)
        String id = doc.sourceType() + "-" + doc.sourceId();
        return Document.builder()
                .id(id)
                .text(doc.text())
                .metadata(Map.of(
                        META_SOURCE_TYPE, doc.sourceType(),
                        META_SOURCE_ID, doc.sourceId()
                ))
                .build();
    }

    private String sourceLabel(Document doc) {
        Object type = doc.getMetadata().get(META_SOURCE_TYPE);
        Object id = doc.getMetadata().get(META_SOURCE_ID);
        return type + "#" + id;
    }

    /** 로그용 — 각 문서의 "출처=유사도점수" 나열. */
    private String scoresOf(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return "(없음)";
        }
        return docs.stream()
                .map(doc -> {
                    Double score = doc.getScore();
                    return sourceLabel(doc) + "=" + (score == null ? "?" : String.format("%.3f", score));
                })
                .collect(Collectors.joining(", "));
    }
}
