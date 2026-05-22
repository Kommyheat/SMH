package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String ask(String userMessage) {
        String url = "https://api.openai.com/v1/responses";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", """
                                        당신은 복약 관리 앱의 상담 챗봇입니다.
                                        역할은 복약 일정 안내, 앱 사용 안내, 일반적인 복약 주의사항 설명입니다.

                                        반드시 지킬 규칙:
                                        1. 의사나 약사처럼 진단, 처방, 복용 중단을 지시하지 마세요.
                                        2. 약물 상호작용, 부작용, 임신, 고령자, 만성질환 관련 질문은 전문가 상담을 권유하세요.
                                        3. 응급 증상은 즉시 119 또는 응급실 방문을 안내하세요.
                                        4. 답변은 한국어로 짧고 명확하게 하세요.
                                        """
                        ),
                        Map.of(
                                "role", "user",
                                "content", userMessage
                        )
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                JsonNode.class
        );

        JsonNode responseBody = response.getBody();

        if (responseBody == null) {
            throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
        }

        return responseBody.path("output").get(0)
                .path("content").get(0)
                .path("text").asText();
    }
}
