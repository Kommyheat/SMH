package org.example.service;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.User;
import org.example.dto.*;
import org.example.dto.UserProfileResponse;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.web-client-id:}")
    private String googleWebClientId;

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

        return LoginResponse.from(user, true);
    }

    @Transactional
    public LoginResponse loginWithGoogle(String idToken) {
        if (googleWebClientId == null || googleWebClientId.trim().isEmpty()) {
            throw new IllegalArgumentException("서버의 GOOGLE_WEB_CLIENT_ID 설정이 필요합니다.");
        }

        GoogleTokenInfo tokenInfo = verifyGoogleIdToken(idToken);
        User user = userRepository.findByEmail(tokenInfo.email())
                .orElseGet(() -> createSocialUser(tokenInfo));

        return LoginResponse.from(user, isProfileCompleted(user));
    }

    @Transactional
    public LoginResponse loginWithKakao(String accessToken) {
        KakaoUserInfo userInfo = verifyKakaoAccessToken(accessToken);
        User user = userRepository.findByEmail(userInfo.email())
                .orElseGet(() -> {
                    try {
                        return createKakaoSocialUser(userInfo);
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByEmail(userInfo.email())
                                .orElseThrow(() -> new IllegalArgumentException("카카오 계정 처리 중 충돌이 발생했습니다. 다시 시도해주세요."));
                    }
                });

        return LoginResponse.from(user, isProfileCompleted(user));
    }

    @Transactional
    public LoginResponse loginWithNaver(String accessToken) {
        NaverUserInfo userInfo = verifyNaverAccessToken(accessToken);
        User user = userRepository.findByEmail(userInfo.email())
                .orElseGet(() -> {
                    try {
                        return createNaverSocialUser(userInfo);
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByEmail(userInfo.email())
                                .orElseThrow(() -> new IllegalArgumentException("네이버 계정 처리 중 충돌이 발생했습니다. 다시 시도해주세요."));
                    }
                });

        return LoginResponse.from(user, isProfileCompleted(user));
    }

    @Transactional
    public LoginResponse completeSocialProfile(Long userId, String name, LocalDate birthDate) {
        User user = findUserById(userId);
        user.updateUser(name == null ? null : name.trim(), birthDate);
        User saved = userRepository.saveAndFlush(user);
        return LoginResponse.from(saved, true);
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

    private GoogleTokenInfo verifyGoogleIdToken(String idToken) {
        try {
            String encodedToken = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedToken))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("유효하지 않은 구글 토큰입니다.");
            }

            JsonNode obj = objectMapper.readTree(response.body());
            String aud = obj.path("aud").asText("");
            String email = obj.path("email").asText("");
            String name = obj.path("name").asText("GoogleUser");
            String sub = obj.path("sub").asText("");

            if (!googleWebClientId.equals(aud)) {
                throw new IllegalArgumentException("앱과 매칭되지 않는 구글 토큰입니다.");
            }
            if (email.isBlank() || sub.isBlank()) {
                throw new IllegalArgumentException("구글 계정 정보가 올바르지 않습니다.");
            }

            return new GoogleTokenInfo(sub, email, name);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("구글 토큰 검증 중 오류가 발생했습니다.");
        }
    }

    private User createSocialUser(GoogleTokenInfo tokenInfo) {
        String loginId = generateUniqueSocialLoginId(tokenInfo.email(), "g_");
        String encodedPassword = passwordEncoder.encode("GOOGLE_" + UUID.randomUUID());
        String linkCode = generateUniqueLinkCode();

        User user = new User(
                loginId,
                encodedPassword,
                tokenInfo.email(),
                tokenInfo.name(),
                LocalDate.of(1970, 1, 1),
                linkCode
        );
        return userRepository.save(user);
    }

    private KakaoUserInfo verifyKakaoAccessToken(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://kapi.kakao.com/v2/user/me"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("유효하지 않은 카카오 토큰입니다.");
            }

            JsonNode obj = objectMapper.readTree(response.body());
            String kakaoId = obj.path("id").asText("");
            String email = obj.path("kakao_account").path("email").asText("");
            String nickname = obj.path("properties").path("nickname").asText("KakaoUser");

            if (kakaoId.isBlank()) {
                throw new IllegalArgumentException("카카오 계정 정보가 올바르지 않습니다.");
            }
            if (email == null || email.isBlank()) {
                email = "kakao_" + kakaoId + "@smh.quest";
            }

            return new KakaoUserInfo(kakaoId, email, nickname);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("카카오 토큰 검증 중 오류가 발생했습니다.");
        }
    }

    private User createKakaoSocialUser(KakaoUserInfo userInfo) {
        String loginId = generateUniqueSocialLoginId(userInfo.email(), "k_");
        String encodedPassword = passwordEncoder.encode("KAKAO_" + UUID.randomUUID());
        String linkCode = generateUniqueLinkCode();
        String displayName = userInfo.nickname() == null ? "" : userInfo.nickname().trim();
        if (displayName.isEmpty()) {
            displayName = "KakaoUser";
        }

        User user = new User(
                loginId,
                encodedPassword,
                userInfo.email(),
                displayName,
                LocalDate.of(1970, 1, 1),
                linkCode
        );
        return userRepository.save(user);
    }

    private NaverUserInfo verifyNaverAccessToken(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openapi.naver.com/v1/nid/me"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("유효하지 않은 네이버 토큰입니다.");
            }

            JsonNode obj = objectMapper.readTree(response.body());
            JsonNode profile = obj.path("response");
            String naverId = profile.path("id").asText("");
            String email = profile.path("email").asText("");
            String name = profile.path("name").asText("NaverUser");
            String birthYear = profile.path("birthyear").asText("");
            String birthday = profile.path("birthday").asText("");

            if (naverId.isBlank()) {
                throw new IllegalArgumentException("네이버 계정 정보가 올바르지 않습니다.");
            }
            if (email == null || email.isBlank()) {
                email = "naver_" + naverId + "@smh.social";
            }
            if (name == null || name.trim().isEmpty()) {
                name = "NaverUser";
            }

            LocalDate birthDate = parseNaverBirthDate(birthYear, birthday);
            return new NaverUserInfo(naverId, email, name.trim(), birthDate);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("네이버 토큰 검증 중 오류가 발생했습니다.");
        }
    }

    private User createNaverSocialUser(NaverUserInfo userInfo) {
        String loginId = generateUniqueSocialLoginId(userInfo.email(), "n_");
        String encodedPassword = passwordEncoder.encode("NAVER_" + UUID.randomUUID());
        String linkCode = generateUniqueLinkCode();
        LocalDate birthDate = userInfo.birthDate() == null ? LocalDate.of(1970, 1, 1) : userInfo.birthDate();

        User user = new User(
                loginId,
                encodedPassword,
                userInfo.email(),
                userInfo.name(),
                birthDate,
                linkCode
        );
        return userRepository.save(user);
    }

    private boolean isProfileCompleted(User user) {
        if (user.getBirthDate() == null) return false;
        return !LocalDate.of(1970, 1, 1).equals(user.getBirthDate());
    }

    private String generateUniqueSocialLoginId(String email, String prefix) {
        String safePrefix = (prefix == null || prefix.isBlank()) ? "g_" : prefix;
        String base = email == null ? "google_user" : email.split("@")[0].replaceAll("[^A-Za-z0-9_]", "");
        if (base.isBlank()) base = "google_user";
        if (base.length() > 20) base = base.substring(0, 20);

        String candidate = safePrefix + base;
        if (!userRepository.existsByLoginId(candidate)) {
            return candidate;
        }

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String merged = safePrefix + base;
        int maxBaseLength = Math.max(1, 30 - suffix.length() - 1);
        if (merged.length() > maxBaseLength) {
            merged = merged.substring(0, maxBaseLength);
        }
        return merged + "_" + suffix;
    }

    private LocalDate parseNaverBirthDate(String birthYear, String birthday) {
        try {
            if (birthYear == null || birthday == null) return null;
            String year = birthYear.trim();
            String mmdd = birthday.trim();
            if (!year.matches("\\d{4}") || !mmdd.matches("\\d{2}-\\d{2}")) return null;
            return LocalDate.parse(year + "-" + mmdd);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record GoogleTokenInfo(String sub, String email, String name) {
    }

    private record KakaoUserInfo(String kakaoId, String email, String nickname) {
    }

    private record NaverUserInfo(String naverId, String email, String name, LocalDate birthDate) {
    }
}
