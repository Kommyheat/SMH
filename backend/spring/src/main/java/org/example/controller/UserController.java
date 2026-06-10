package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.LinkCodeResponse;
import org.example.dto.MessageResponse;
import org.example.dto.UserPasswordUpdateRequest;
import org.example.dto.UserProfileResponse;
import org.example.dto.UserUpdateRequest;
import org.example.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 내 정보 조회 (임시로 userId를 request param으로 받음)
    @GetMapping("/user")
    public UserProfileResponse getMyProfile(@RequestParam Long userId) {
        return userService.getMyProfile(userId);
    }

    // 내 정보 수정
    @PatchMapping("/user")
    public UserProfileResponse updateMyProfile(@RequestParam Long userId,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateMyProfile(userId, request);
    }

    // 비밀번호 변경
    @PatchMapping("/user/password")
    public MessageResponse changePassword(@RequestParam Long userId,
                                          @Valid @RequestBody UserPasswordUpdateRequest request) {
        userService.changePassword(userId, request);
        return new MessageResponse("비밀번호가 변경되었습니다.");
    }

    // 내 연동코드 조회
    @GetMapping("/me/link-code")
    public LinkCodeResponse getMyLinkCode(@RequestParam Long userId) {
        return userService.getMyLinkCode(userId);
    }
}