package org.example.dto;

import lombok.Getter;
import org.example.domain.User;

@Getter
public class LoginResponse {

    private Long id;
    private String loginId;
    private String name;

    public LoginResponse(User user) {
        this.id = user.getId();
        this.loginId = user.getLoginId();
        this.name = user.getName();
    }

    public static LoginResponse from(User user) {
        return new LoginResponse(user);
    }
}
