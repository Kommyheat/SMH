package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

//회원 비밀번호 수정 dto

@Getter
@NoArgsConstructor
public class UserPasswordUpdateRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 6, message = "새 비밀번호는 최소 6자 이상이어야 합니다.")
    private String newPassword;

}
