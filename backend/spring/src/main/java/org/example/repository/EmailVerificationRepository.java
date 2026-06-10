package org.example.repository;

import org.example.domain.EmailVerification;
import org.example.domain.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByIdDesc(String email, VerificationPurpose purpose);
    Optional<EmailVerification> findTopByEmailAndPurposeAndVerifiedTokenOrderByIdDesc(
            String email,
            VerificationPurpose purpose,
            String verifiedToken
    );
}
