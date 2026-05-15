package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

//회원정보 수정(비밀번호는 여기서 수정X)

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자 10~11자리여야 합니다.")
    private String phone;

    @NotNull(message = "생년월일은 필수입니다.")
    @Past(message = "생년월일은 미래 날짜일 수 없습니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}
