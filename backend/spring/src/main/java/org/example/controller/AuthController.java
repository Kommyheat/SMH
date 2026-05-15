package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.EmailCodeSendRequest;
import org.example.dto.EmailCodeVerifyRequest;
import org.example.dto.EmailCodeVerifyResponse;
import org.example.dto.FindLoginIdByCodeRequest;
import org.example.dto.FindLoginIdRequest;
import org.example.dto.FindLoginIdResponse;
import org.example.dto.LoginResponse;
import org.example.dto.MessageResponse;
import org.example.dto.ResetPasswordByCodeRequest;
import org.example.dto.ResetPasswordRequest;
import org.example.dto.ResetPasswordResponse;
import org.example.dto.UserCreateRequest;
import org.example.dto.UserLoginRequest;
import org.example.dto.UserProfileResponse;
import org.example.service.EmailVerificationService;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse signup(@Valid @RequestBody UserCreateRequest request) {
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody UserLoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/check-login-id")
    public MessageResponse checkLoginIdAvailability(@RequestParam String loginId) {
        boolean available = userService.isLoginIdAvailable(loginId);
        return new MessageResponse(available ? "AVAILABLE" : "DUPLICATED");
    }

    @PostMapping("/find-login-id")
    public FindLoginIdResponse findLoginId(@Valid @RequestBody FindLoginIdRequest request) {
        return userService.findLoginId(request);
    }

    @PostMapping("/reset-password")
    public ResetPasswordResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request);
    }

    @PostMapping("/email/send-code")
    public MessageResponse sendEmailCode(@Valid @RequestBody EmailCodeSendRequest request) {
        return emailVerificationService.sendCode(request);
    }

    @PostMapping("/email/verify-code")
    public EmailCodeVerifyResponse verifyEmailCode(@Valid @RequestBody EmailCodeVerifyRequest request) {
        return emailVerificationService.verifyCode(request);
    }

    @PostMapping("/find-login-id/by-code")
    public FindLoginIdResponse findLoginIdByCode(@Valid @RequestBody FindLoginIdByCodeRequest request) {
        String loginId = emailVerificationService.findLoginIdByVerifiedEmail(
                request.getEmail(),
                request.getVerifyToken()
        );
        return new FindLoginIdResponse(loginId);
    }

    @PostMapping("/reset-password/by-code")
    public MessageResponse resetPasswordByCode(@Valid @RequestBody ResetPasswordByCodeRequest request) {
        emailVerificationService.resetPasswordByVerifiedEmail(
                request.getEmail(),
                request.getVerifyToken(),
                request.getNewPassword()
        );
        return new MessageResponse("비밀번호가 변경되었습니다.");
    }
}
