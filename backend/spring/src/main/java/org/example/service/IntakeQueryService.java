package org.example.service;



import lombok.RequiredArgsConstructor;
import org.example.domain.IntakeLog;
import org.example.domain.User;
import org.example.dto.IntakeItemResponse;
import org.example.dto.PatientIntakeStatusResponse;
import org.example.repository.IntakeLogRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntakeQueryService {

    private final IntakeLogRepository intakeLogRepository;
    private final UserRepository userRepository;

    public PatientIntakeStatusResponse getTodayPatientIntakeStatus(Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("피보호자를 찾을 수 없습니다."));

        List<IntakeLog> logs = intakeLogRepository
                .findByMedicationSchedule_Medication_User_IdAndDate(patientId, LocalDate.now());

        List<IntakeItemResponse> items = logs.stream()
                .map(log -> new IntakeItemResponse(
                        log.getMedicationSchedule().getMedication().getMedicationName(),
                        log.getMedicationSchedule().getScheduledTime(),
                        log.getStatus().name(),
                        log.getTakenAt(),
                        log.getMemo(),
                        log.getMedicationSchedule().getQuantity(),
                        log.getMedicationSchedule().getUnit()
                ))
                .toList();

        return new PatientIntakeStatusResponse(
                patient.getId(),
                patient.getName(),
                items
        );
    }
}
