package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.EmailVerification;
import org.example.domain.VerificationPurpose;
import org.example.dto.EmailCodeSendRequest;
import org.example.dto.EmailCodeVerifyRequest;
import org.example.dto.EmailCodeVerifyResponse;
import org.example.dto.MessageResponse;
import org.example.repository.EmailVerificationRepository;
import org.example.domain.User;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final Random RANDOM = new Random();

    private final SesV2Client sesV2Client;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Transactional
    public MessageResponse sendCode(EmailCodeSendRequest request) {
        String code = generateCode();
        String codeHash = hash(code);

        EmailVerification verification = new EmailVerification(
                request.getEmail().trim().toLowerCase(),
                request.getPurpose(),
                codeHash,
                LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES)
        );
        emailVerificationRepository.save(verification);

        String subject = buildSubject(request.getPurpose());
        String textBody = buildTextBody(code, request.getPurpose());
        sendEmail(request.getEmail(), subject, textBody, code);

        return new MessageResponse("인증코드를 이메일로 발송했습니다.");
    }

    @Transactional
    public EmailCodeVerifyResponse verifyCode(EmailCodeVerifyRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        VerificationPurpose purpose = request.getPurpose();

        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByIdDesc(normalizedEmail, purpose)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 이력이 없습니다."));

        if (verification.isVerified()) {
            throw new IllegalArgumentException("이미 사용된 인증코드입니다.\n다시 발송해주세요.");
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증코드가 만료되었습니다.\n다시 발송해주세요.");
        }
        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다.\n코드를 다시 발송해주세요.");
        }

        String inputHash = hash(request.getCode().trim());
        if (!verification.getCodeHash().equals(inputHash)) {
            verification.increaseAttempts();
            throw new IllegalArgumentException("인증코드가 일치하지 않습니다.");
        }

        String verifyToken = UUID.randomUUID().toString().replace("-", "");
        verification.markVerified(verifyToken);
        return new EmailCodeVerifyResponse(true, verifyToken);
    }

    @Transactional
    public String findLoginIdByVerifiedEmail(String email, String verifyToken) {
        EmailVerification verification = loadVerifiedRecord(email, VerificationPurpose.FIND_ID, verifyToken);

        User user = userRepository.findTopByEmailOrderByIdDesc(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾지 못했습니다."));

        verification.consume();
        return user.getLoginId();
    }

    @Transactional
    public void resetPasswordByVerifiedEmail(String email, String verifyToken, String newPassword) {
        EmailVerification verification = loadVerifiedRecord(email, VerificationPurpose.RESET_PASSWORD, verifyToken);

        User user = userRepository.findTopByEmailOrderByIdDesc(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보를 찾지 못했습니다."));

        user.changeEncodedPassword(passwordEncoder.encode(newPassword));
        verification.consume();
    }

    private EmailVerification loadVerifiedRecord(String email, VerificationPurpose purpose, String verifyToken) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndPurposeAndVerifiedTokenOrderByIdDesc(
                        email.trim().toLowerCase(),
                        purpose,
                        verifyToken
                )
                .orElseThrow(() -> new IllegalArgumentException("인증 정보가 유효하지 않습니다."));

        if (!verification.isVerified()) {
            throw new IllegalArgumentException("인증이 완료되지 않았습니다.");
        }
        if (verification.isConsumed()) {
            throw new IllegalArgumentException("이미 사용된 인증 정보입니다.");
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증코드가 만료되었습니다.\n다시 인증해주세요.");
        }
        return verification;
    }

    private void sendEmail(String toEmail, String subject, String textBody, String code) {
        String htmlBody = buildHtmlBody(subject, code);
        Destination destination = Destination.builder().toAddresses(toEmail).build();

        Content sesSubject = Content.builder().charset("UTF-8").data(subject).build();
        Content sesTextBody = Content.builder().charset("UTF-8").data(textBody).build();
        Content sesHtmlBody = Content.builder().charset("UTF-8").data(htmlBody).build();
        Body body = Body.builder()
                .text(sesTextBody)
                .html(sesHtmlBody)
                .build();
        Message message = Message.builder().subject(sesSubject).body(body).build();

        EmailContent emailContent = EmailContent.builder().simple(message).build();

        SendEmailRequest sendRequest = SendEmailRequest.builder()
                .fromEmailAddress(fromEmail)
                .destination(destination)
                .content(emailContent)
                .build();

        try {
            sesV2Client.sendEmail(sendRequest);
        } catch (SesV2Exception e) {
            throw new IllegalArgumentException("이메일 발송에 실패했습니다.");
        }
    }

    private String buildSubject(VerificationPurpose purpose) {
        return switch (purpose) {
            case FIND_ID -> "[약드림] 아이디 찾기 인증코드";
            case RESET_PASSWORD -> "[약드림] 비밀번호 재설정 인증코드";
        };
    }

    private String buildTextBody(String code, VerificationPurpose purpose) {
        String purposeText = purpose == VerificationPurpose.FIND_ID
                ? "[약드림] 아이디 찾기 인증코드"
                : "[약드림] 비밀번호 재설정 인증코드";

        return "약드림 인증 안내\n\n"
                + purposeText + "\n\n"
                + "아래 코드를 앱 화면에 입력해주세요.\n"
                + code + "\n\n"
                + "인증코드는 " + CODE_EXPIRE_MINUTES + "분 동안 유효합니다.";
    }

    private String buildHtmlBody(String subject, String code) {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>약드림 인증 메일</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f6fb;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Noto Sans KR',sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f6fb;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="max-width:560px;width:100%%;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(17,24,39,.08);">
                          <tr>
                            <td style="background:#00245B;padding:18px 24px;color:#ffffff;font-size:18px;font-weight:700;">
                              약드림 인증 안내
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:24px;">
                              <p style="margin:0 0 10px;font-size:16px;font-weight:700;">%s</p>
                              <p style="margin:0 0 18px;font-size:14px;line-height:1.65;color:#4b5563;">
                                아래 코드를 앱 화면에 입력해주세요.
                              </p>
                              <div style="margin:0 0 18px;padding:14px 16px;border:1px dashed #ff7f73;border-radius:12px;background:#fff7f6;text-align:center;">
                                <span style="font-size:30px;font-weight:800;letter-spacing:6px;color:#ff7f73;">%s</span>
                              </div>
                              <p style="margin:0;font-size:13px;color:#6b7280;">
                                인증코드는 %d분 동안 유효합니다.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:14px 24px;background:#f9fafb;color:#9ca3af;font-size:12px;line-height:1.6;">
                              본 메일은 발신 전용입니다.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(subject, code, CODE_EXPIRE_MINUTES);
    }

    private String generateCode() {
        int value = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(value);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("인증코드 해시 처리 실패");
        }
    }
}
