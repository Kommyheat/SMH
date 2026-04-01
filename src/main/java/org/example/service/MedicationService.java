package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.Medication;
import org.example.domain.MedicationStatus;
import org.example.domain.User;
import org.example.repository.MedicationRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final UserRepository userRepository;

    // 약 등록
    @Transactional
    public Long createMedication(Long userId, String medicationName, String ingredient,
                                 String purpose, LocalDate startDate, LocalDate endDate) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다. id=" + userId));

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("복용 시작일은 종료일보다 늦을 수 없습니다.");
        }

        Medication medication = new Medication(user, medicationName, purpose, startDate, endDate);
        return medicationRepository.save(medication).getId();
    }

    // 약 단건 조회
    public Medication getMedication(Long medicationId) {
        return medicationRepository.findById(medicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약물 정보가 존재하지 않습니다. id=" + medicationId));
    }

    // 사용자별 약물 전체 조회
    public List<Medication> getMedicationsByUser(Long userId) {
        return medicationRepository.findByUserId(userId);
    }

    // 사용자별 상태 조회 (복용중, 복용완료, 복용중단)
    public List<Medication> getMedicationsByUserAndStatus(Long userId, MedicationStatus status) {
        return medicationRepository.findByUserIdAndStatus(userId, status);
    }

    // 현재 복용중인 약 조회 (오늘 날짜 기준)
    public List<Medication> getCurrentMedications(Long userId) {
        LocalDate today = LocalDate.now();
        return medicationRepository.findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                userId, today, today
        );
    }

    // 약 정보 수정
    @Transactional
    public void updateMedication(Long medicationId, Long userId, String medicationName,
                                 String ingredient, String purpose,
                                 LocalDate startDate, LocalDate endDate) {

        Medication medication = medicationRepository.findByIdAndUserId(medicationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약물 정보가 존재하지 않거나 사용자 권한이 없습니다."));

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("복용 시작일은 종료일보다 늦을 수 없습니다.");
        }

        medication.updateMedicationInfo(medicationName, ingredient, purpose, startDate, endDate);
    }

    // 약 상태 변경
    @Transactional
    public void changeMedicationStatus(Long medicationId, Long userId, MedicationStatus status) {
        Medication medication = medicationRepository.findByIdAndUserId(medicationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약물 정보가 존재하지 않거나 사용자 권한이 없습니다."));

        medication.changeStatus(status);
    }

    // 약 정보 삭제
    @Transactional
    public void deleteMedication(Long medicationId, Long userId) {
        Medication medication = medicationRepository.findByIdAndUserId(medicationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약물 정보가 존재하지 않거나 사용자 권한이 없습니다."));

        medicationRepository.delete(medication);
    }
}