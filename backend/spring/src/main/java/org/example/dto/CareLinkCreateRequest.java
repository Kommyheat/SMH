package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

//보호자가 피보호자의 코드를 입력할 때
@Getter
@NoArgsConstructor
public class CareLinkCreateRequest {

    @NotBlank(message = "피보호자의 고유코드 입력은 필수입니다.")
    private String linkCode;
}
