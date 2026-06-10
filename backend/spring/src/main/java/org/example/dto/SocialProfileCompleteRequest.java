package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SocialProfileCompleteRequest {

    @NotNull(message = "userId는 필수입니다.")
    private Long userId;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "생년월일은 필수입니다.")
    @Past(message = "생년월일은 미래 날짜일 수 없습니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}

