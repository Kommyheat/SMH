package org.example.repository;

import org.example.domain.CareLink;
import org.example.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CareLinkRepository extends JpaRepository<CareLink, Long> {

    Optional<CareLink> findByCaregiverAndPatient(User caregiver, User patient);

    List<CareLink> findByPatient(User patient);

    List<CareLink> findByCaregiver(User caregiver);
}