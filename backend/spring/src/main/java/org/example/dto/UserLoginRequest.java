package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

//로그인 dto

@Getter
@NoArgsConstructor
public class UserLoginRequest {

    @NotBlank(message = "아이디 입력은 필수입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다.")
    private String password;
}
