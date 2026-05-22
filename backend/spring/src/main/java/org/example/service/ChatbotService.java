package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatbotResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final OpenAiClient openAiClient;

    public ChatbotResponse reply(String message) {
        String answer = openAiClient.ask(message);
        return new ChatbotResponse(answer);
    }
}
