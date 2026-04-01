package org.example.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;               //로그인 id

    @Column(nullable = false)
    private String password;             //암호화 해야함

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, unique = true, length = 20)
    private String linkCode;

    public User(String userId, String password, String name, String phone, LocalDate birthDate) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.linkCode = generateLinkCode();
    }

    public void updateUser(String name, String phone, LocalDate birthDate) {
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void regenerateLinkCode() {
        this.linkCode = generateLinkCode();
    }

    private String generateLinkCode() {
        return java.util.UUID.randomUUID().toString().substring(0,8);
    }
}