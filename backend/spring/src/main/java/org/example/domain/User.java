package org.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, unique = true, length = 20)
    private String linkCode;    //연동 고유코드

    public User(String loginId, String encodedPassword, String email, String name, LocalDate birthDate, String linkCode) {
        this.loginId = loginId;
        this.password = encodedPassword;
        this.email = email;
        this.name = name;
        this.birthDate = birthDate;
        this.linkCode = linkCode;
    }

    public void updateUser(String name, LocalDate birthDate) {
        this.name = name;
        this.birthDate = birthDate;
    }  //일반 로그인 한 사람은 수정 가능 소셜로그인은 불가능

    //암호화된 비밀번호만 받는다
    public void changeEncodedPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

}