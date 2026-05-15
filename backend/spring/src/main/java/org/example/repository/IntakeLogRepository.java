package org.example.repository;

import org.example.domain.IntakeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

//특정사용자와 오늘 날짜의 복약 로그를 가져옴
public interface IntakeLogRepository extends JpaRepository<IntakeLog, Long> {

    List<IntakeLog> findByMedicationSchedule_Medication_User_IdAndDate(Long userId, LocalDate date);
}