package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.User;
import org.example.dto.*;
import org.example.dto.UserProfileResponse;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public UserProfileResponse createUser(UserCreateRequest request) {
        validateDuplicateLoginId(request.getLoginId());
        validatePasswordConfirmation(request.getPassword(), request.getPasswordOk());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        String linkCode = generateUniqueLinkCode();

        User user = new User(
                request.getLoginId(),
                encodedPassword,
                request.getEmail(),
                request.getName(),
                request.getBirthDate(),
                linkCode
        );

        User savedUser = userRepository.save(user);
        return new UserProfileResponse(savedUser);
    }

    // 로그인
    public LoginResponse login(UserLoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return LoginResponse.from(user);
    }

    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    public FindLoginIdResponse findLoginId(FindLoginIdRequest request) {
        User user = userRepository.findByNameAndEmailAndBirthDate(
                        request.getName(),
                        request.getEmail(),
                        request.getBirthDate()
                )
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾지 못했습니다."));

        return new FindLoginIdResponse(user.getLoginId());
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByLoginIdAndNameAndEmailAndBirthDate(
                        request.getLoginId(),
                        request.getName(),
                        request.getEmail(),
                        request.getBirthDate()
                )
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾지 못했습니다."));

        String temporaryPassword = generateTemporaryPassword();
        user.changeEncodedPassword(passwordEncoder.encode(temporaryPassword));

        return new ResetPasswordResponse(temporaryPassword);
    }

    // 내 정보 조회
    public UserProfileResponse getMyProfile(Long loginUserId) {
        User user = findUserById(loginUserId);
        return new UserProfileResponse(user);
    }

    // 내 정보 수정
    @Transactional
    public UserProfileResponse updateMyProfile(Long loginUserId, UserUpdateRequest request) {
        User user = findUserById(loginUserId);

        user.updateUser(
                request.getName(),
                request.getBirthDate()
        );

        return new UserProfileResponse(user);
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(Long loginUserId, UserPasswordUpdateRequest request) {
        User user = findUserById(loginUserId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호와 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        user.changeEncodedPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    // 내 연동 코드 조회
    public LinkCodeResponse getMyLinkCode(Long loginUserId) {
        User user = findUserById(loginUserId);
        return LinkCodeResponse.from(user.getLinkCode());
    }

    private void validateDuplicateLoginId(String loginId) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
    }

    private String generateUniqueLinkCode() {
        String linkCode;

        do {
            linkCode = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        } while (userRepository.existsByLinkCode(linkCode));

        return linkCode;
    }

    private void validatePasswordConfirmation(String password, String passwordOk) {
        if (password == null || !password.equals(passwordOk)) {
            throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
        }
    }

    private String generateTemporaryPassword() {
        String random = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);

        return "Tmp!" + random;
    }
}
