package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.User;
import org.example.dto.*;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        User user = new User(
                request.getUserId(),
                passwordEncoder.encode(request.getPassword()), //비밀번호는 암호화
                request.getName(),
                request.getPhone(),
                request.getBirthDate()
        );

        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser);
    }

    // 로그인
    public UserResponse login(UserLoginRequest request) {
        User user = userRepository.findByUserId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return new UserResponse(user);
    }

    // 사용자 단건 조회
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        return new UserResponse(user);
    }

    //사용자 정보 수정(이름,전번,생년월일)
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        user.updateUser(
                request.getName(),
                request.getPhone(),
                request.getBirthDate()
        );

        return new UserResponse(user);
    }

    //비밀번호 변경
    @Transactional
    public void changePassword(Long id, UserPasswordUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));   //비밀번호 검증

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호와 일치하지 않습니다.");                  //기존 비밀번호와 일치하는지 체크
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));                   //새 비밀번호 암호화 후 저장
    }

    //연동 코드 조회(보호자와 연결 시 사용)
    public LinkCodeResponse getLinkCode(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        return new LinkCodeResponse(user.getLinkCode());
    }

    //연동코드 재발급(기존 코드 무효화 후 새로운 코드 생성)
    @Transactional
    public LinkCodeResponse regenerateLinkCode(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        user.regenerateLinkCode();

        return new LinkCodeResponse(user.getLinkCode());
    }
}