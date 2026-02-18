package com.back.catchmate.mcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CatchMateApiDocTool {

    private final WebClient webClient = WebClient.create("http://localhost:8080");

    @Tool(description = "CatchMate 프로젝트의 최신 API 명세서(Swagger JSON)를 가져옵니다.")
    public String getCatchmateApiDocs() {  // Function 반환 X, 직접 String 반환
        try {
            return webClient.get()
                    .uri("/v3/api-docs")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            return "API 문서를 불러오지 못했습니다. 메인 서버가 8080 포트에서 켜져 있는지 확인하세요.";
        }
    }
}
