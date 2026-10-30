package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {

    @NotBlank(message = "idToken은 필수입니다.")
    private String idToken;
}

