package org.example.repository;

import org.example.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);
    Optional<User> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByLinkCode(String linkCode);

    Optional<User> findByLinkCode(String linkCode);

    Optional<User> findByNameAndEmailAndBirthDate(String name, String email, LocalDate birthDate);

    Optional<User> findByLoginIdAndNameAndEmailAndBirthDate(String loginId, String name, String email, LocalDate birthDate);

    Optional<User> findTopByEmailOrderByIdDesc(String email);
}
