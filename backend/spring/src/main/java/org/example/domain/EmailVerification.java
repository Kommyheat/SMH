package org.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationPurpose purpose;

    @Column(nullable = false, length = 128)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean verified;

    @Column(length = 100)
    private String verifiedToken;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private boolean consumed;

    private LocalDateTime consumedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public EmailVerification(
            String email,
            VerificationPurpose purpose,
            String codeHash,
            LocalDateTime expiresAt
    ) {
        this.email = email;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.verified = false;
        this.consumed = false;
        this.createdAt = LocalDateTime.now();
    }

    public void increaseAttempts() {
        this.attempts += 1;
    }

    public void markVerified(String token) {
        this.verified = true;
        this.verifiedToken = token;
        this.verifiedAt = LocalDateTime.now();
    }

    public void consume() {
        this.consumed = true;
        this.consumedAt = LocalDateTime.now();
    }
}
