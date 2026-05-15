package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.domain.VerificationPurpose;

@Getter
@Setter
public class EmailCodeVerifyRequest {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식을 확인해주세요.")
    private String email;

    @NotNull(message = "인증 목적이 필요합니다.")
    private VerificationPurpose purpose;

    @NotBlank(message = "인증코드를 입력해주세요.")
    private String code;
}
