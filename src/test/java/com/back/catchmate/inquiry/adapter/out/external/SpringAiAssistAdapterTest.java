package com.back.catchmate.inquiry.adapter.out.external;

import com.back.catchmate.inquiry.adapter.out.external.SpringAiAssistAdapter.LlmDraft;
import com.back.catchmate.inquiry.application.port.out.dto.AnswerDraft;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpringAiAssistAdapterTest {

    private final ChatClient chatClient = mock(ChatClient.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final SpringAiAssistAdapter adapter =
            new SpringAiAssistAdapter(chatClient, vectorStore, 4, 0.6);

    @Test
    void 근거_문서가_없으면_LLM_호출없이_fallback() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        AnswerDraft draft = adapter.draftAnswer("환불 어떻게 하나요?");

        assertThat(draft.grounded()).isFalse();
        assertThat(draft.sources()).isEmpty();
        verifyNoInteractions(chatClient); // threshold 미달 시 비용 드는 LLM 호출을 하지 않는다
    }

    @Test
    void 검색됐고_LLM이_관련있다고_판정하면_grounded_true_와_출처를_반환() {
        stubSearchHit();
        stubLlm(new LlmDraft(true, "환불은 마이페이지에서 신청하실 수 있습니다."));

        AnswerDraft draft = adapter.draftAnswer("환불 어떻게 하나요?");

        assertThat(draft.grounded()).isTrue();
        assertThat(draft.draftText()).contains("마이페이지");
        assertThat(draft.sources()).containsExactly("NOTICE#12");
    }

    @Test
    void 검색은_됐지만_LLM이_무관하다고_판정하면_fallback() {
        stubSearchHit(); // 유사 문서는 잡히지만
        stubLlm(new LlmDraft(false, "")); // LLM 이 실제로는 답이 안 된다고 판정

        AnswerDraft draft = adapter.draftAnswer("비밀번호를 잊어버렸어요.");

        assertThat(draft.grounded()).isFalse();
        assertThat(draft.sources()).isEmpty();
    }

    private void stubSearchHit() {
        Document hit = Document.builder()
                .id("NOTICE-12")
                .text("환불은 마이페이지에서 신청합니다.")
                .metadata("sourceType", "NOTICE")
                .metadata("sourceId", 12L)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit));
    }

    private void stubLlm(LlmDraft result) {
        ChatClient.ChatClientRequestSpec requestSpec =
                mock(ChatClient.ChatClientRequestSpec.class, org.mockito.Mockito.RETURNS_SELF);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.entity(eq(LlmDraft.class))).thenReturn(result);
    }
}
