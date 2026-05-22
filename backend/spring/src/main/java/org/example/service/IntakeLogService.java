package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.*;
import org.example.dto.IntakeLogResponse;
import org.example.repository.IntakeLogRepository;
import org.example.repository.MedicationScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntakeLogService {

    private final IntakeLogRepository intakeLogRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;

    @Transactional
    public Long takeMedication(Long scheduleId, Long userId, LocalDate date, String memo) {

        MedicationSchedule schedule = medicationScheduleRepository
                .findByIdAndMedicationUserId(scheduleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 복약 일정이 없거나 사용자 권한이 없습니다."));
        // 로그 추가
        System.out.println("[INTAKE] schedule found id=" + schedule.getId());

        IntakeLog log = intakeLogRepository
                .findByMedicationSchedule_IdAndDate(scheduleId, date)
                .orElseGet(() -> {
                    System.out.println("[INTAKE] no existing log, creating new IntakeLog");

                    return new IntakeLog(schedule, date);
                });
        // 로그 추가
        System.out.println("[INTAKE] intakeLog prepared status=" + log.getStatus());

        log.markAsTaken(memo);
    // 로그 추가
        System.out.println("[INTAKE] markAsTaken complete status=" + log.getStatus());
        IntakeLog saved = intakeLogRepository.save(log);
        System.out.println("[INTAKE] save complete id=" + saved.getId());

        // 추가: 모든 복용 완료 여부 체크 후 COMPLETED 변경
        checkAndCompleteIfDone(schedule.getMedication());
        return saved.getId();
    }
    /**
     * 추가: 복용 기간 내 모든 스케줄이 TAKEN이면 medication.status = COMPLETED
     */
    private void checkAndCompleteIfDone(Medication medication) {
        LocalDate startDate = medication.getStartDate();
        LocalDate endDate = medication.getEndDate();
        LocalDate today = LocalDate.now();

        // 복용 기간이 아직 안 끝났으면 체크 안 함
        if (today.isBefore(endDate)) {
            System.out.println("[INTAKE] 복용 기간 미종료, COMPLETED 체크 스킵");
            return;
        }

        // 해당 약의 모든 스케줄 조회
        List<MedicationSchedule> schedules =
                medicationScheduleRepository.findByMedication(medication);

        if (schedules.isEmpty()) return;

        // 기간 내 총 예상 복용 횟수
        long totalDays = startDate.until(endDate, java.time.temporal.ChronoUnit.DAYS) + 1;
        long totalExpected = schedules.size() * totalDays;

        // 실제 TAKEN 횟수
        long takenCount = intakeLogRepository
                .countByMedicationSchedule_MedicationAndStatus(
                        medication, IntakeStatus.TAKEN);

        System.out.println("[INTAKE] totalExpected=" + totalExpected
                + " / takenCount=" + takenCount);

        if (takenCount >= totalExpected) {
            medication.changeStatus(MedicationStatus.COMPLETED);
            System.out.println("[INTAKE] 모든 복용 완료 → COMPLETED 변경: "
                    + medication.getMedicationName());
        }
    }

    @Transactional
    public void cancelTake(Long scheduleId, Long userId, LocalDate date) {

        medicationScheduleRepository
                .findByIdAndMedicationUserId(scheduleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 복약 일정이 없거나 사용자 권한이 없습니다."));

        System.out.println("[INTAKE] schedule validation passed");
        IntakeLog log = intakeLogRepository
                .findByMedicationSchedule_IdAndDate(scheduleId, date)
                .orElseThrow(() -> new IllegalArgumentException("복약 기록이 없습니다."));
        // 로그 추가
        System.out.println("[INTAKE] intake log found id=" + log.getId());
        log.cancelTaken();
        System.out.println("[INTAKE] cancelTaken complete");
    }

    public List<IntakeLogResponse> getDailyLogs(Long userId, LocalDate date) {
        return intakeLogRepository
                .findByMedicationSchedule_Medication_User_IdAndDate(userId, date)
                .stream()
                .map(IntakeLogResponse::new)
                .toList();
    }

    public List<IntakeLogResponse> getMonthlyLogs(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return intakeLogRepository
                .findByMedicationSchedule_Medication_User_IdAndDateBetween(userId, startDate, endDate)
                .stream()
                .map(IntakeLogResponse::new)
                .toList();
    }

}
