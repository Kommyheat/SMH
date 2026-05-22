package org.example.repository;

import org.example.domain.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.domain.Medication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {

    List<MedicationSchedule> findByMedicationId(Long medicationId);

    @Query("""
        select s
        from MedicationSchedule s
        join fetch s.medication m
        where m.user.id = :userId
        order by s.scheduledTime asc
    """)
    List<MedicationSchedule> findByMedicationUserIdWithMedication(@Param("userId") Long userId);

    @Query("""
        select s
        from MedicationSchedule s
        join fetch s.medication m
        where m.user.id = :userId
          and m.startDate <= :date
          and m.endDate >= :date
          and m.status = org.example.domain.MedicationStatus.ACTIVE
        order by s.scheduledTime asc
    """)
    List<MedicationSchedule> findActiveSchedulesByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    @Query("""
    select s
    from MedicationSchedule s
    join fetch s.medication m
    where s.id = :scheduleId
      and m.user.id = :userId
""")
    Optional<MedicationSchedule> findByIdAndMedicationUserId(
            @Param("scheduleId") Long scheduleId,
            @Param("userId") Long userId
    );

    //추가
    List<MedicationSchedule> findByMedication(Medication medication);
}
