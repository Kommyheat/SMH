package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.Medication;
import org.example.domain.MedicationSchedule;
import org.example.dto.MedicationScheduleRequest;
import org.example.dto.MedicationScheduleResponse;
import org.example.repository.MedicationRepository;
import org.example.repository.MedicationScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationScheduleService {

    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationRepository medicationRepository;

    @Transactional
    public Long createSchedule(Long userId, MedicationScheduleRequest request) {

        Medication medication = medicationRepository.findByIdAndUserId(request.getMedicationId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약 정보가 없거나 사용자 권한이 없습니다."));

        MedicationSchedule schedule = new MedicationSchedule(
                medication,
                request.getTimeSlot(),
                request.getScheduledTime(),
                request.getQuantity(),
                request.getUnit(),
                request.getNotificationEnabled()
        );

        return medicationScheduleRepository.save(schedule).getId();
    }

    public List<MedicationScheduleResponse> getSchedulesByMedication(Long userId, Long medicationId) {
        Medication medication = medicationRepository.findByIdAndUserId(medicationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약 정보가 없거나 사용자 권한이 없습니다."));

        return medicationScheduleRepository.findByMedicationId(medication.getId()).stream()
                .map(MedicationScheduleResponse::new)
                .toList();
    }

    public List<MedicationScheduleResponse> getSchedulesByUser(Long userId) {
        return medicationScheduleRepository.findByMedicationUserIdWithMedication(userId).stream()
                .map(MedicationScheduleResponse::new)
                .toList();
    }

    @Transactional
    public void updateSchedule(Long scheduleId, Long userId, MedicationScheduleRequest request) {

        MedicationSchedule schedule = medicationScheduleRepository.findByIdAndMedicationUserId(scheduleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 복약 일정이 없거나 사용자 권한이 없습니다."));

        schedule.updateSchedule(
                request.getTimeSlot(),
                request.getScheduledTime(),
                request.getQuantity(),
                request.getUnit(),
                request.getNotificationEnabled()
        );
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        MedicationSchedule schedule = medicationScheduleRepository.findByIdAndMedicationUserId(scheduleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 복약 일정이 없거나 사용자 권한이 없습니다."));

        medicationScheduleRepository.delete(schedule);
    }
}
