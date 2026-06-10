package org.example.repository;

import org.example.domain.IntakeLog;
import org.example.domain.IntakeStatus;
import org.example.domain.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IntakeLogRepository extends JpaRepository<IntakeLog, Long> {

    Optional<IntakeLog> findByMedicationSchedule_IdAndDate(
            Long medicationScheduleId,
            LocalDate date
    );

    boolean existsByMedicationSchedule_IdAndDate(
            Long medicationScheduleId,
            LocalDate date
    );

    List<IntakeLog> findByDate(LocalDate date);

    List<IntakeLog> findByMedicationSchedule_Medication_User_IdAndDate(
            Long userId,
            LocalDate date
    );

    List<IntakeLog> findByMedicationSchedule_Medication_User_IdAndDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    long countByMedicationSchedule_MedicationAndStatus(
            Medication medication,
            IntakeStatus status
    );
}
