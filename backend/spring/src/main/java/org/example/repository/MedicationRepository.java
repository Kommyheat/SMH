package org.example.repository;

import org.example.domain.Medication;
import org.example.domain.MedicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    //사용자 전체 약물 조회
    List<Medication> findByUserId(Long userId);

    //특정 사용자, 특정 약물 조회
    Optional<Medication> findByIdAndUserId(Long medicationId, Long userId);

    //상태별 약물 조회 (복용중, 복용완료, 복용중단)
    List<Medication> findByUserIdAndStatus(Long userId, MedicationStatus status);

    //오늘 날짜 기준으로 현재 복용중인 약 조회
    List<Medication> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate today1, LocalDate today2
    );

    // 복용 시작 예정 약 조회 (알림용)
    List<Medication> findByStartDate(LocalDate date);

    // 복용 종료 예정 약 조회 (알림용)
    List<Medication> findByEndDate(LocalDate date);
}