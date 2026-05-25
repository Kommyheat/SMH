package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoLoginRequest {

    @NotBlank(message = "accessToken은 필수입니다.")
    private String accessToken;
}
