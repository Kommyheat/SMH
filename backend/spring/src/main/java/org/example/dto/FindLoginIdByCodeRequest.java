package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindLoginIdByCodeRequest {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식을 확인해주세요.")
    private String email;

    @NotBlank(message = "인증 토큰이 필요합니다.")
    private String verifyToken;
}
