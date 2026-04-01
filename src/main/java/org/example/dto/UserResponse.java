package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.example.domain.User;

import java.time.LocalDate;

@Getter
public class UserResponse {

    private Long id;
    private String userId;
    private String name;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String linkCode;

    public UserResponse(User user) {
        this.id = user.getId();
        this.userId = user.getUserId();
        this.name = user.getName();
        this.phone = user.getPhone();
        this.birthDate = user.getBirthDate();
        this.linkCode = user.getLinkCode();
    }
}