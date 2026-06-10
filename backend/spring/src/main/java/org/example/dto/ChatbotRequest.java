package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatbotRequest(
        @NotBlank(message = "메시지 입력은 필수입니다.")
        String message
) {
}
