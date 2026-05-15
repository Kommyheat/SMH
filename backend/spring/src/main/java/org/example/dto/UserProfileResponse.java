package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.example.domain.User;

import java.time.LocalDate;

@Getter
public class UserProfileResponse {

    private Long id;
    private String loginId;
    private String name;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String email;


    public UserProfileResponse(User user) {
        this.id = user.getId();
        this.loginId = user.getLoginId();
        this.name = user.getName();
        this.birthDate = user.getBirthDate();
        this.email = user.getEmail();
    }
}
