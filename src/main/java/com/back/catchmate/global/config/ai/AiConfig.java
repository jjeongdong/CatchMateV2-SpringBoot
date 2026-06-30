package com.back.catchmate.global.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 빈 설정 — RAG(답변 초안 어시스트)에 필요한 외부 도구 3종을 등록하는 단일 지점.
 *
 * <p>여기가 벤더 격리 지점이다. OpenAI → 다른 벤더로 교체해도 이 파일의 빈 정의만 바꾸면 되고,
 * 어댑터({@code SpringAiAssistAdapter})는 {@link ChatClient} / {@link VectorStore} 인터페이스에만
 * 의존하므로 영향받지 않는다.
 *
 * <ul>
 *   <li>{@link ChatClient} — gpt-4.1 호출 (답변 초안 생성)</li>
 *   <li>{@link EmbeddingModel} — text-embedding-3-small. openai 스타터가 자동 등록하므로 주입만 받는다.</li>
 *   <li>{@link VectorStore} — 개발용 {@link SimpleVectorStore}(인메모리). 운영 전환 시 Redis Vector 로 교체.</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
