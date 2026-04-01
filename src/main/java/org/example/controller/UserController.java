package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.example.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    //회원가입
    @PostMapping("/register")
    public UserResponse register( @RequestBody @Valid UserCreateRequest request) {
        return userService.createUser(request);
    }

    //로그인
    @PostMapping("/login")
    public UserResponse login(@RequestBody @Valid UserLoginRequest request) {
        return userService.login(request);
    }

    //사용자 단건 조회 (id로 정보 조회)
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    //사용자 정보 수정(이름,전번,생년월일)
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id,
                                   @RequestBody @Valid UserUpdateRequest request) {
        return userService.updateUser(id, request);
    }

    //비밀번호 변경
    @PatchMapping("/{id}/password")
    public String changePassword(@PathVariable Long id,
                                 @RequestBody @Valid UserPasswordUpdateRequest request) {
        userService.changePassword(id, request);
        return "비밀번호가 변경되었습니다.";
    }

    //보호자 연동 코드 조회
    @GetMapping("/{id}/link-code")
    public LinkCodeResponse getLinkCode(@PathVariable Long id) {
        return userService.getLinkCode(id);
    }

    //연동코드 재발급(기존 코드 무효화 후 새로운 코드 생성)
    @PatchMapping("/{id}/link-code")
    public LinkCodeResponse regenerateLinkCode(@PathVariable Long id) {
        return userService.regenerateLinkCode(id);
    }
}
