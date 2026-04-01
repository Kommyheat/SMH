package org.example.repository;

import org.example.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String loginId);

    boolean existsByUserId(String userId);

    boolean existsByLinkCode(String linkCode);
}