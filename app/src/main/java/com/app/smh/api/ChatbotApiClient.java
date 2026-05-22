package com.app.smh.api;

import com.app.smh.BuildConfig;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();
    private final String baseUrl = BuildConfig.SERVER_BASE_URL;

    public ChatbotResponse sendMessage(ChatbotRequest requestDto) throws IOException, ApiException {
        String body = gson.toJson(requestDto);

        Request request = new Request.Builder()
                .url(baseUrl + "/api/chatbot/message")
                .post(RequestBody.create(body, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new ApiException("챗봇 응답 요청에 실패했습니다.");
            }

            return gson.fromJson(responseBody, ChatbotResponse.class);
        }
    }

    public static class ChatbotRequest {
        public String message;

        public ChatbotRequest(String message) {
            this.message = message;
        }
    }

    public static class ChatbotResponse {
        public String answer;
    }

    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
    }
}
