package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

//회원가입 dto

@Getter
@NoArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
            message = "비밀번호는 8자 이상, 영문/숫자/특수문자를 모두 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 10, message = "이름은 10자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotNull(message = "생년월일은 필수입니다.")
    @Past(message = "생년월일은 미래 날짜일 수 없습니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;


    //비밀번호 확인
    @NotBlank(message = "비밀번호가 일치하지 않습니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
    private String passwordOk;
}
