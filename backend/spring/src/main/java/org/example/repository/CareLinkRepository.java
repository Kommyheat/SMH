package org.example.repository;

import org.example.domain.CareLink;
import org.example.domain.CareLinkStatus;
import org.example.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CareLinkRepository extends JpaRepository<CareLink, Long> {

    // 기존
    boolean existsByCaregiverAndPatientAndStatus(
            User caregiver, User patient, CareLinkStatus status);

    List<CareLink> findByCaregiverAndStatus(
            User caregiver, CareLinkStatus status);

    Optional<CareLink> findByCaregiverAndPatientAndStatus(
            User caregiver, User patient, CareLinkStatus status);

    Optional<CareLink> findByIdAndCaregiverAndStatus(
            Long id, User caregiver, CareLinkStatus status);

    @Query("SELECT c FROM CareLink c " +
            "WHERE c.caregiver = :user OR c.patient = :user " +
            "ORDER BY c.id DESC")
    List<CareLink> findByCaregiversOrPatient(@Param("user") User user);
}
