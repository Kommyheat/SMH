package org.example.dto;

import lombok.Getter;
import org.example.domain.User;

import java.time.LocalDate;

@Getter
public class LoginResponse {

    private Long id;
    private String loginId;
    private String name;
    private String email;
    private LocalDate birthDate;
    private boolean profileCompleted;

    public LoginResponse(User user, boolean profileCompleted) {
        this.id = user.getId();
        this.loginId = user.getLoginId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.birthDate = user.getBirthDate();
        this.profileCompleted = profileCompleted;
    }

    public static LoginResponse from(User user, boolean profileCompleted) {
        return new LoginResponse(user, profileCompleted);
    }
}
